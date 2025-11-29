package com.example.paisacheck360



import android.annotation.SuppressLint

import android.content.Intent

import android.os.Bundle

import android.widget.*

import androidx.appcompat.app.AppCompatActivity

import android.provider.Settings

import java.security.MessageDigest

import com.google.firebase.database.*



class LoginActivity : AppCompatActivity() {



    private lateinit var database: DatabaseReference

    private lateinit var androidID: String



    @SuppressLint("MissingInflatedId")

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)



// Get Android ID

        androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)



// Firebase DB

        database = FirebaseDatabase.getInstance().reference.child("users")



        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        val loginBtn = findViewById<Button>(R.id.loginBtn)

        val resetBtn = findViewById<Button>(R.id.registerBtn)



        loginBtn.setOnClickListener {

            val password = passwordInput.text.toString()

            if (password.isNotEmpty()) {

                val hashed = hashPassword(password)



// Check in Firebase

                database.child(androidID).get().addOnSuccessListener {

                    if (it.exists()) {

                        val savedHash = it.child("passwordHash").value.toString()

                        if (savedHash == hashed) {

                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this,MainActivity::class.java))

                            finish()

                        } else {

                            Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()

                        }

                    } else {

// First time user → save hash

                        database.child(androidID).child("passwordHash").setValue(hashed)

                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, MainActivity::class.java))

                        finish()

                    }

                }

            } else {

                Toast.makeText(this, "Enter a password!", Toast.LENGTH_SHORT).show()

            }

        }



// Reset password

        resetBtn.setOnClickListener {

            passwordInput.setText("")

            database.child(androidID).removeValue().addOnSuccessListener {

                Toast.makeText(this, "Password reset. Enter new password.", Toast.LENGTH_SHORT).show()

            }

        }

    }



    private fun hashPassword(password: String): String {

        val digest = MessageDigest.getInstance("SHA-256")

        val hashBytes = digest.digest(password.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }

    }

}