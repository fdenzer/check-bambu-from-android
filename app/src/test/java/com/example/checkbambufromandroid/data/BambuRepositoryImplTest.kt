package com.example.checkbambufromandroid.data

import com.example.checkbambufromandroid.network.BambuLabApiService
import com.example.checkbambufromandroid.network.models.LoginRequest
import com.example.checkbambufromandroid.network.models.LoginResponse
import com.example.checkbambufromandroid.network.models.PrinterStatusResponse
import com.example.checkbambufromandroid.util.assertFlowEmits
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.io.IOException

@ExperimentalCoroutinesApi
class BambuRepositoryImplTest {

    // Rule for MainCoroutineDispatcher will be set if using Main dispatcher in ViewModels/Repo.
    // For this repository, not strictly needed as it uses Dispatchers.IO implicitly via Retrofit.
    // @get:Rule
    // val mainCoroutineRule = MainCoroutineRule() // If you create such a rule

    private lateinit var apiService: BambuLabApiService
    private lateinit var repository: BambuRepositoryImpl

    private val loginRequest = LoginRequest("test@example.com", "password")

    @Before
    fun setUp() {
        apiService = mock()
        repository = BambuRepositoryImpl(apiService)
    }

    @Test
    fun `login success direct`() = runTest {
        val successResponse = LoginResponse("Login successful!", accessTokenAvailable = true)
        whenever(apiService.login(loginRequest)).thenReturn(Response.success(successResponse))

        val expectedValues = listOf(
            Resource.Loading<LoginResponse>(),
            Resource.Success(successResponse, needsVerification = false)
        )
        assertFlowEmits(repository.login(loginRequest), expectedValues)
    }

    @Test
    fun `login needs verification`() = runTest {
        val needsVerificationResponse = LoginResponse("Verification code sent.", needsVerification = true)
        whenever(apiService.login(loginRequest)).thenReturn(Response.success(needsVerificationResponse))

        val expectedValues = listOf(
            Resource.Loading<LoginResponse>(),
            Resource.Success(needsVerificationResponse, needsVerification = true)
        )
        assertFlowEmits(repository.login(loginRequest), expectedValues)
    }

    @Test
    fun `login http error`() = runTest {
        val errorResponseBody = "{\"error\":\"Bad Request\"}".toResponseBody("application/json".toMediaTypeOrNull())
        whenever(apiService.login(loginRequest)).thenReturn(Response.error(400, errorResponseBody))

        val result = repository.login(loginRequest).first { it !is Resource.Loading } // Get first non-loading emit

        assertTrue(result is Resource.Error)
        assertTrue(result.message!!.contains("Login failed: {\"error\":\"Bad Request\"}"))
    }

    @Test
    fun `login network error`() = runTest {
        whenever(apiService.login(loginRequest)).thenThrow(IOException("Network down"))

        val result = repository.login(loginRequest).first { it !is Resource.Loading }

        assertTrue(result is Resource.Error)
        assertTrue(result.message!!.contains("Login failed: Network error."))
    }


    @Test
    fun `getPrinterStatus success`() = runTest {
        val statusResponse = PrinterStatusResponse(formattedStatus = "Idle")
        whenever(apiService.getPrinterStatus()).thenReturn(Response.success(statusResponse))

        val expectedValues = listOf(
            Resource.Loading<PrinterStatusResponse>(),
            Resource.Success(statusResponse)
        )
        assertFlowEmits(repository.getPrinterStatus(), expectedValues)
    }

    @Test
    fun `getPrinterStatus unauthorized error`() = runTest {
        val errorResponseBody = "Unauthorized".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(apiService.getPrinterStatus()).thenReturn(Response.error(401, errorResponseBody))

        val result = repository.getPrinterStatus().first { it !is Resource.Loading }

        assertTrue(result is Resource.Error)
        assertTrue(result.message!!.contains("Unauthorized: Session might have expired."))
    }

    @Test
    fun `getPrinterStatus network error`() = runTest {
        whenever(apiService.getPrinterStatus()).thenThrow(IOException("Network failure"))

        val result = repository.getPrinterStatus().first { it !is Resource.Loading }

        assertTrue(result is Resource.Error)
        assertTrue(result.message!!.contains("Failed to get status: Network error."))
    }
}

// Helper to get MediaType or null if string is invalid
private fun String.toMediaTypeOrNull() = try {
    okhttp3.MediaType.get(this)
} catch (e: IllegalArgumentException) {
    null
}
