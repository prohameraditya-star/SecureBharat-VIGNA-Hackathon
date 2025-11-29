package com.example.paisacheck360

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScamViewModel : ViewModel() {

    private val _scamSummaryLiveData = MutableLiveData<ScamSummary>()
    val scamSummaryLiveData: LiveData<ScamSummary> get() = _scamSummaryLiveData

    fun updateSummary(fraudMessages: List<FraudMessage>) {
        val now = System.currentTimeMillis()
        var last7Days = 0
        var sms = 0
        var upi = 0
        var latestHeadline = "No scams detected"

        for (msg in fraudMessages) {
            if (now - msg.timestamp <= 7L * 24 * 60 * 60 * 1000) {
                last7Days++
                when (msg.type.uppercase()) {
                    "SMS" -> sms++
                    "UPI" -> upi++
                }
            }
        }

        if (fraudMessages.isNotEmpty()) {
            latestHeadline = fraudMessages[0].message
        }

        _scamSummaryLiveData.postValue(
            ScamSummary(latestHeadline, last7Days, sms, upi)
        )
    }
}
