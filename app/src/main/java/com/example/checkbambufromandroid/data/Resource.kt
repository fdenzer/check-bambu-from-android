package com.example.checkbambufromandroid.data

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val needsVerification: Boolean? = null // Specific for login 2FA
) {
    class Success<T>(data: T, needsVerification: Boolean? = null) : Resource<T>(data = data, needsVerification = needsVerification)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data = data, message = message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
