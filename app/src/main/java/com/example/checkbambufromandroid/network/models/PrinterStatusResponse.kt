package com.example.checkbambufromandroid.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrinterStatusResponse(
    @SerialName("formatted_status")
    val formattedStatus: String? = null,
    @SerialName("progress")
    val progress: String? = null, // Assuming string, could be Int or Double
    @SerialName("estimated_finish_time")
    val estimatedFinishTime: String? = null,
    // Add other fields as needed based on actual API response
    // For example:
    // @SerialName("nozzle_temp")
    // val nozzleTemp: Double? = null,
    // @SerialName("bed_temp")
    // val bedTemp: Double? = null,
    @SerialName("message") // For error messages from the backend if any
    val message: String? = null
)
