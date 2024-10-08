package com.example.nutrichief.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrichief.MainActivity
import com.example.nutrichief.R
import com.example.nutrichief.datamodels.UserLogin
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.login_emp_btn)

        loginBtn.setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.login_email_field).text.toString()
            val password = findViewById<TextInputEditText>(R.id.login_password_field).text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                val userLogin = UserLogin(email, password)

                Log.d("Login", userLogin.toString())

                GlobalScope.launch(Dispatchers.IO) {
                    loginUser(userLogin) { response, jwtToken, errorMessage ->
                        Log.e("confirm", "login btn clicked")
                        runOnUiThread {
                            if (response.isSuccessful && jwtToken != null) {
                                // Login successful, save JWT to SharedPreferences
                                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putString("jwt_token", jwtToken)
                                editor.apply()

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Login successful",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Move to MainActivity
                                val mainActivity = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(mainActivity)
                                finish()

                            } else {
                                // Login failed
                                Toast.makeText(
                                    this@LoginActivity,
                                    errorMessage ?: "Failed to login",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(
        customer: UserLogin,
        callback: (Response, String?, String?) -> Unit
    ) {
        GlobalScope.launch {
            try {
                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jacksonObjectMapper().writeValueAsString(customer)
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("http://mealplanner.aqgxexddffeza6gn.australiaeast.azurecontainer.io/api/v1/account/login")
                    .post(requestBody)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string()

                        if (response.isSuccessful && responseBody != null) {
                            // Parse JSON response to get JWT token from "data" -> "access_token"
                            try {
                                val json = jacksonObjectMapper().readTree(responseBody)
                                val accessToken = json.get("data").get("access_token").asText()
                                callback(response, accessToken, null)
                            } catch (e: Exception) {
                                callback(response, null, "Failed to parse response")
                            }
                        } else {
                            callback(response, null, "Invalid credentials")
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        // Handle network failure
                        callback(Response.Builder().code(-1).build(), null, "Network error: ${e.message}")
                    }
                })

            } catch (e: Exception) {
                callback(Response.Builder().code(-1).build(), null, "Error: ${e.message}")
            }
        }
    }
}
