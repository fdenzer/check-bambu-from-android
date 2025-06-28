package com.example.checkbambufromandroid.data

import com.example.checkbambufromandroid.network.BambuLabApiService
import com.example.checkbambufromandroid.network.models.LoginRequest
import com.example.checkbambufromandroid.network.models.LoginResponse
import com.example.checkbambufromandroid.network.models.PrinterStatusResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface BambuRepository {
    fun login(loginRequest: LoginRequest): Flow<Resource<LoginResponse>>
    fun getPrinterStatus(): Flow<Resource<PrinterStatusResponse>>
}

@Singleton
class BambuRepositoryImpl @Inject constructor(
    private val apiService: BambuLabApiService
) : BambuRepository {

    override fun login(loginRequest: LoginRequest): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.login(loginRequest)
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                if (loginResponse.needsVerification == true) {
                    // Backend indicates 2FA is needed
                    emit(Resource.Success(data = loginResponse, needsVerification = true))
                } else if (loginResponse.accessTokenAvailable == true) {
                    // Login successful directly or after 2FA
                    emit(Resource.Success(data = loginResponse, needsVerification = false))
                } else {
                    // Should not happen based on README, but handle defensively
                    emit(Resource.Error(message = loginResponse.message ?: "Unknown login error"))
                }
            } else {
                // HTTP error (4xx, 5xx)
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody.isNullOrEmpty()) {
                    "Login failed: ${response.code()} ${response.message()}"
                } else {
                    // Try to parse errorBody if it's JSON, otherwise use as is
                    // For simplicity, using it as is. A proper app might parse it.
                    "Login failed: $errorBody"
                }
                emit(Resource.Error(message = errorMessage))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(message = "Login failed: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error(message = "Login failed: Network error. Please check your connection."))
        } catch (e: Exception) {
            emit(Resource.Error(message = "Login failed: An unexpected error occurred. ${e.message}"))
        }
    }

    override fun getPrinterStatus(): Flow<Resource<PrinterStatusResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getPrinterStatus()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(data = response.body()!!))
            } else {
                // Handle specific error codes like 401 for token expired
                if (response.code() == 401) {
                    emit(Resource.Error(message = "Unauthorized: Session might have expired. Please login again."))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody.isNullOrEmpty()) {
                        "Failed to get status: ${response.code()} ${response.message()}"
                    } else {
                        "Failed to get status: $errorBody"
                    }
                    emit(Resource.Error(message = errorMessage))
                }
            }
        } catch (e: HttpException) {
            emit(Resource.Error(message = "Failed to get status: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error(message = "Failed to get status: Network error. Please check your connection."))
        } catch (e: Exception) {
            emit(Resource.Error(message = "Failed to get status: An unexpected error occurred. ${e.message}"))
        }
    }
}
