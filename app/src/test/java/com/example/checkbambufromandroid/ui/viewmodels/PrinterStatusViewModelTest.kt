package com.example.checkbambufromandroid.ui.viewmodels

import app.cash.turbine.test
import com.example.checkbambufromandroid.data.BambuRepository
import com.example.checkbambufromandroid.data.Resource
import com.example.checkbambufromandroid.network.models.PrinterStatusResponse
import com.example.checkbambufromandroid.util.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class PrinterStatusViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: BambuRepository
    private lateinit var viewModel: PrinterStatusViewModel

    @Before
    fun setUp() {
        repository = mock()
    }

    // Helper to create ViewModel after mocking repository for init block
    private fun initializeViewModel() {
        viewModel = PrinterStatusViewModel(repository)
    }

    @Test
    fun `init fetches printer status and emits Loading then Success`() = runTest {
        val statusResponse = PrinterStatusResponse(formattedStatus = "Idle")
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Success(statusResponse)
        ))

        initializeViewModel() // ViewModel's init block calls fetchPrinterStatus

        viewModel.statusState.test {
            // Initial state of VM might be emitted before flow collection in test if not careful
            // However, Turbine should capture emissions post-subscription.
            // The init {} block dispatches a coroutine, so the states will come through.

            var currentState = awaitItem() // Could be default initial or first Loading
            if (!currentState.isLoading && currentState.printerStatus == null) { // Default initial state
                 currentState = awaitItem() // Should be Loading
            }
            assertTrue(currentState.isLoading)
            assertNull(currentState.printerStatus) // Loading, so no data yet

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(statusResponse, successState.printerStatus)
            assertNull(successState.error)

            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).getPrinterStatus()
    }

    @Test
    fun `init fetches printer status and emits Loading then Error`() = runTest {
        val errorMessage = "Network Error"
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        ))

        initializeViewModel()

        viewModel.statusState.test {
            var currentState = awaitItem()
            if (!currentState.isLoading && currentState.error == null) {
                 currentState = awaitItem()
            }
            assertTrue(currentState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNull(errorState.printerStatus)
            assertEquals(errorMessage, errorState.error)
            assertFalse(errorState.navigateToLogin)

            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).getPrinterStatus()
    }

    @Test
    fun `init fetches printer status and emits Loading then Unauthorized Error`() = runTest {
        val errorMessage = "Unauthorized: Token expired"
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage) // RepositoryImpl adds "Unauthorized:" prefix
        ))

        initializeViewModel()

        viewModel.statusState.test {
            var currentState = awaitItem()
            if (!currentState.isLoading && currentState.error == null) {
                 currentState = awaitItem()
            }
            assertTrue(currentState.isLoading)


            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNull(errorState.printerStatus)
            assertEquals(errorMessage, errorState.error)
            assertTrue(errorState.navigateToLogin) // Key check for unauthorized

            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).getPrinterStatus()
    }


    @Test
    fun `fetchPrinterStatus emits Loading then Success`() = runTest {
        // Mock for init
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(Resource.Success(PrinterStatusResponse("Initial"))))
        initializeViewModel()
        viewModel.statusState.test { awaitItem(); cancelAndIgnoreRemainingEvents() } // Consume init emissions


        val newStatusResponse = PrinterStatusResponse(formattedStatus = "Printing")
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Success(newStatusResponse)
        ))

        viewModel.fetchPrinterStatus()

        viewModel.statusState.test {
            // First awaitItem might be the old "Initial" state or the new Loading.
            // Turbine collects current value then new values.
            var currentState = awaitItem() // Could be "Initial" state or Loading
            if (!currentState.isLoading) currentState = awaitItem() // If it was "Initial", next is Loading

            assertTrue(currentState.isLoading)
            // Previous status might still be there if isLoading is just a flag on existing state
            // assertEquals("Initial", currentState.printerStatus?.formattedStatus) // This depends on how state is updated

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(newStatusResponse, successState.printerStatus)
            assertNull(successState.error)

            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).getPrinterStatus() // Called twice (init and manual)
    }

    @Test
    fun `userLogout sets navigateToLogin true`() = runTest {
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(Resource.Success(PrinterStatusResponse("Initial"))))
        initializeViewModel()
        viewModel.statusState.test { awaitItem(); cancelAndIgnoreRemainingEvents() }


        viewModel.userLogout()

        viewModel.statusState.test {
            val logoutState = awaitItem()
            assertTrue(logoutState.navigateToLogin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetNavigation sets navigateToLogin false and clears error`() = runTest {
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(Resource.Error("Unauthorized"))) // To set navigateToLogin and error
        initializeViewModel()
        viewModel.statusState.test { skipItems(1); awaitItem(); cancelAndIgnoreRemainingEvents() } // consume loading and error state

        // Verify current state before reset
        val beforeReset = viewModel.statusState.value
        assertTrue(beforeReset.navigateToLogin)
        assertNotNull(beforeReset.error)


        viewModel.resetNavigation()
        viewModel.statusState.test {
            val resetState = awaitItem()
            assertFalse(resetState.navigateToLogin)
            assertNull(resetState.error) // Error also cleared
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError clears error message`() = runTest {
        whenever(repository.getPrinterStatus()).thenReturn(flowOf(Resource.Error("Test Error")))
        initializeViewModel()
        viewModel.statusState.test {
            skipItems(1) // Skip loading
            assertEquals("Test Error", awaitItem().error)

            viewModel.clearError()
            assertNull(awaitItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
