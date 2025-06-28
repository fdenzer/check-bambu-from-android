package com.example.checkbambufromandroid.network

import com.example.checkbambufromandroid.network.models.LoginRequest
import com.example.checkbambufromandroid.network.models.LoginResponse
import com.example.checkbambufromandroid.network.models.PrinterStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BambuLabApiService {

    @POST("api/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("api/printer-status")
    suspend fun getPrinterStatus(): Response<PrinterStatusResponse>

}
