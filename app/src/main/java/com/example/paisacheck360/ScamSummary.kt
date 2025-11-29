package com.example.paisacheck360

data class ScamSummary(
    val headline: String,
    val last7DaysCount: Int,
    val smsCount: Int,
    val upiCount: Int
)
