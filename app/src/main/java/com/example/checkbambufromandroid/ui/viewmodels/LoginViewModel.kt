package com.example.checkbambufromandroid.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkbambufromandroid.data.BambuRepository
import com.example.checkbambufromandroid.data.Resource
import com.example.checkbambufromandroid.network.models.LoginRequest
import com.example.checkbambufromandroid.network.models.LoginResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val needsVerification: Boolean = false,
    val loginSuccess: Boolean = false // Used to trigger navigation
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: BambuRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginScreenState())
    val loginState: StateFlow<LoginScreenState> = _loginState.asStateFlow()

    // Store email and password temporarily for the verification step
    private var tempEmail: String? = null
    private var tempPassword: String? = null

    fun login(email: String, password: String) {
        tempEmail = email
        tempPassword = password
        val loginRequest = LoginRequest(email = email, password = password)
        executeLogin(loginRequest)
    }

    fun verifyCode(verificationCode: String) {
        if (tempEmail == null || tempPassword == null) {
            _loginState.value = LoginScreenState(error = "Session error. Please try logging in again.")
            return
        }
        val loginRequest = LoginRequest(
            email = tempEmail!!,
            password = tempPassword!!,
            verificationCode = verificationCode
        )
        executeLogin(loginRequest)
    }

    private fun executeLogin(loginRequest: LoginRequest) {
        viewModelScope.launch {
            repository.login(loginRequest)
                .onEach { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _loginState.value = LoginScreenState(isLoading = true)
                        }
                        is Resource.Success -> {
                            val response = resource.data
                            if (response?.needsVerification == true) {
                                _loginState.value = LoginScreenState(
                                    needsVerification = true,
                                    error = response.message // "Verification code was sent..."
                                )
                            } else if (response?.accessTokenAvailable == true) {
                                _loginState.value = LoginScreenState(loginSuccess = true, error = response.message) // "Login successful!" or "Verification successful!"
                            } else {
                                // Should not happen based on repository logic, but handle defensively
                                _loginState.value = LoginScreenState(error = response?.message ?: "Unknown success state")
                            }
                        }
                        is Resource.Error -> {
                            _loginState.value = LoginScreenState(
                                error = resource.message,
                                // Preserve needsVerification if the error occurred during verification step
                                needsVerification = _loginState.value.needsVerification
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun resetNavigation() {
        _loginState.value = _loginState.value.copy(loginSuccess = false, error = null) // Clear error after navigation or on screen entry
    }

    fun clearError() {
        _loginState.value = _loginState.value.copy(error = null)
    }
}
