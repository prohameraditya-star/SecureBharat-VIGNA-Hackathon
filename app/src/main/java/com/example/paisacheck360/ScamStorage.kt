package com.example.paisacheck360

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object ScamStorage {

    private const val PREFS_NAME = "SecureBharatPrefs"
    private const val KEY_SCAM_LOGS = "scam_logs"

    fun addScamLog(context: Context, text: String, risk: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logs = JSONArray(prefs.getString(KEY_SCAM_LOGS, "[]"))

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timestamp = sdf.format(Date())

        val newLog = JSONObject().apply {
            put("text", text.take(80)) // limit preview to 80 chars
            put("risk", risk)
            put("time", timestamp)
        }

        logs.put(newLog)
        prefs.edit().putString(KEY_SCAM_LOGS, logs.toString()).apply()
    }

    fun getScamLogs(context: Context): List<JSONObject> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logs = JSONArray(prefs.getString(KEY_SCAM_LOGS, "[]"))
        val list = mutableListOf<JSONObject>()
        for (i in 0 until logs.length()) {
            list.add(logs.getJSONObject(i))
        }
        return list
    }

    fun clearLogs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SCAM_LOGS).apply()
    }
}
