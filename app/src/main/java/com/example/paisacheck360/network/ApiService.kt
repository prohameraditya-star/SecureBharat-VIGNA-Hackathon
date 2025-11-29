package com.example.paisacheck360.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 🔹 Request body
data class SmsRequest(val message: String)

// 🔹 Response body (💥 You MUST add this if missing)
data class SmsResponse(
    val label: String,
    val confidence: Double
)

// 🔹 API Interface
interface ApiService {
    @POST("predict")
    fun predictSms(@Body request: SmsRequest): Call<SmsResponse>
}
