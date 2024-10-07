package com.example.nutrichief.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.nutrichief.MainActivity
import com.example.nutrichief.R
import com.example.nutrichief.datamodels.User
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
                val userLogin = UserLogin(
                    email,
                    password,
                )

                Log.d("Login", userLogin.toString())

                GlobalScope.launch(Dispatchers.IO) {
                    loginUser(userLogin) { response, errorMessage ->
                        Log.e("confirm", "login btn clicked")
                        runOnUiThread {
                            if (response.isSuccessful) {
                                // Login successful
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Login successful",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                val mainActivity = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(mainActivity)

                            } else {
                                // Login failed
                                if (errorMessage != null) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        errorMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.d("error", errorMessage)
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Failed to login",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                }
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun loginUser(customer: UserLogin, callback: (Response, String?) -> Unit) {
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
                        callback(response, responseBody)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        // Handle network failure
                        //                        callback(Response.Builder().code(-1).build(), e.message)
                    }
                })


            } catch (e: Exception) {
                // Handle other exceptions
                //                callback(Response.Builder().code(-1).build(), e.message)
            }
        }
    }
}