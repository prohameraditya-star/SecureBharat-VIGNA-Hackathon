package com.example.paisacheck360


import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable local persistence for RTDB
        try { FirebaseDatabase.getInstance().setPersistenceEnabled(true) } catch (_: Exception) {}
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d("App", "Anon auth ok: ${auth.currentUser?.uid}")
                else Log.e("App", "Anon auth failed", task.exception)
            }
           }
        }
}