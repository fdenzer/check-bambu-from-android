package com.example.checkbambufromandroid.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("message")
    val message: String,
    @SerialName("accessTokenAvailable")
    val accessTokenAvailable: Boolean? = null,
    @SerialName("needsVerification")
    val needsVerification: Boolean? = null
)
