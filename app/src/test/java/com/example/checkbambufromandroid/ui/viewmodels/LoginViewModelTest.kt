package com.example.checkbambufromandroid.ui.viewmodels

import app.cash.turbine.test
import com.example.checkbambufromandroid.data.BambuRepository
import com.example.checkbambufromandroid.data.Resource
import com.example.checkbambufromandroid.network.models.LoginRequest
import com.example.checkbambufromandroid.network.models.LoginResponse
import com.example.checkbambufromandroid.util.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: BambuRepository
    private lateinit var viewModel: LoginViewModel

    private val email = "test@example.com"
    private val password = "password"
    private val verificationCode = "123456"

    @Before
    fun setUp() {
        repository = mock()
        viewModel = LoginViewModel(repository)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val initialState = LoginScreenState()
        assertEquals(initialState, viewModel.loginState.value)
    }

    @Test
    fun `login emits Loading then Success for direct login`() = runTest {
        val loginRequest = LoginRequest(email, password)
        val successResponse = LoginResponse("Success", accessTokenAvailable = true)
        whenever(repository.login(loginRequest)).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Success(successResponse)
        ))

        viewModel.login(email, password)

        viewModel.loginState.test {
            assertEquals(LoginScreenState(isLoading = true), awaitItem()) // Loading from VM after action
            val successState = awaitItem() // Success state
            assertFalse(successState.isLoading)
            assertTrue(successState.loginSuccess)
            assertEquals("Success", successState.error) // Success message is passed via error field
            assertFalse(successState.needsVerification)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).login(loginRequest)
    }

    @Test
    fun `login emits Loading then NeedsVerification`() = runTest {
        val loginRequest = LoginRequest(email, password)
        val needsVerificationResponse = LoginResponse("Needs Code", needsVerification = true)
        whenever(repository.login(loginRequest)).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Success(needsVerificationResponse)
        ))

        viewModel.login(email, password)

        viewModel.loginState.test {
            assertEquals(LoginScreenState(isLoading = true), awaitItem())
            val needsVerificationState = awaitItem()
            assertFalse(needsVerificationState.isLoading)
            assertFalse(needsVerificationState.loginSuccess)
            assertTrue(needsVerificationState.needsVerification)
            assertEquals("Needs Code", needsVerificationState.error)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).login(loginRequest)
    }

    @Test
    fun `login emits Loading then Error`() = runTest {
        val loginRequest = LoginRequest(email, password)
        val errorMessage = "Invalid credentials"
        whenever(repository.login(loginRequest)).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Error(errorMessage)
        ))

        viewModel.login(email, password)

        viewModel.loginState.test {
            assertEquals(LoginScreenState(isLoading = true), awaitItem())
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertFalse(errorState.loginSuccess)
            assertEquals(errorMessage, errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).login(loginRequest)
    }

    @Test
    fun `verifyCode emits Loading then Success`() = runTest {
        // First login to set tempEmail and tempPassword
        viewModel.login(email, password)
        // Consume initial login flow if necessary, or ensure mock is ready for verify
        whenever(repository.login(any())).thenReturn(flowOf(Resource.Success(LoginResponse("dummy", needsVerification = true))))
        viewModel.loginState.test { skipItems(1); awaitItem() } // Consume loading and needs verification

        val verifyRequest = LoginRequest(email, password, verificationCode)
        val successResponse = LoginResponse("Verify Success", accessTokenAvailable = true)
        whenever(repository.login(verifyRequest)).thenReturn(flowOf(
            Resource.Loading(),
            Resource.Success(successResponse)
        ))

        viewModel.verifyCode(verificationCode)

        viewModel.loginState.test {
            // Initial state might be needsVerification=true, error="dummy"
            // We are interested in states after verifyCode is called.
            // The first state after verifyCode will be isLoading=true, needsVerification=true (preserved)
            var currentState = awaitItem()
            if (!currentState.isLoading) currentState = awaitItem() // Handle existing state before loading

            assertEquals(LoginScreenState(isLoading = true, needsVerification = true, error = currentState.error), currentState)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertTrue(successState.loginSuccess)
            assertEquals("Verify Success", successState.error)
            // needsVerification might still be true from previous state or reset depending on exact logic,
            // but loginSuccess=true is the key. Let's check it becomes false if login is successful.
            // Based on current VM: if (response?.accessTokenAvailable == true) { _loginState.value = LoginScreenState(loginSuccess = true, error = response.message) }
            // This means needsVerification will be reset to false (default of LoginScreenState).
            assertFalse(successState.needsVerification)

            cancelAndIgnoreRemainingEvents()
        }
        verify(repository).login(verifyRequest)
    }

    @Test
    fun `verifyCode without prior login fails`() = runTest {
        viewModel.verifyCode(verificationCode)
        viewModel.loginState.test {
            val errorState = awaitItem()
            assertEquals("Session error. Please try logging in again.", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `resetNavigation sets loginSuccess to false and clears error`() = runTest {
        // Set a state that would be after a successful login
        val successResponse = LoginResponse("Success", accessTokenAvailable = true)
        whenever(repository.login(any())).thenReturn(flowOf(Resource.Success(successResponse)))
        viewModel.login(email, password)
        viewModel.loginState.test {
            skipItems(1) // Skip initial and loading
            val successState = awaitItem()
            assertTrue(successState.loginSuccess)
            assertEquals("Success", successState.error)

            viewModel.resetNavigation()
            val resetState = awaitItem()
            assertFalse(resetState.loginSuccess)
            assertNull(resetState.error) // Error is also cleared
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError clears error message`() = runTest {
         whenever(repository.login(any())).thenReturn(flowOf(Resource.Error("Test Error")))
        viewModel.login(email, password)
        viewModel.loginState.test {
            skipItems(1) // Skip initial and loading
            assertEquals("Test Error", awaitItem().error)

            viewModel.clearError()
            assertNull(awaitItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
