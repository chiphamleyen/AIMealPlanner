package com.example.nutrichief.view

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.example.nutrichief.MainActivity
import com.example.nutrichief.R
import com.example.nutrichief.datamodels.User
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.log

class RegisterActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private lateinit var genderList: Spinner
    private var selectedGender: String? = null
    private val genders = arrayOf("Male", "Female")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val registerBtn = findViewById<Button>(R.id.next_emp_btn)

        genderList = findViewById(R.id.gender)
        genderList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedGender = parent?.getItemAtPosition(position) as? String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedGender = null
            }
        }

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        genderList.adapter = adapter

        val signInTextView = findViewById<TextView>(R.id.signInTextView)
        signInTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        registerBtn.setOnClickListener {
            val fullName = findViewById<TextInputEditText>(R.id.fullname).text.toString()
            val email = findViewById<TextInputEditText>(R.id.email).text.toString()
            val gender = selectedGender.toString()
            val dateOfBirthText = findViewById<TextInputEditText>(R.id.dateofbirth).text.toString()
            val password = findViewById<TextInputEditText>(R.id.pwd).text.toString()
//            val email = "chie.bow.gu@gmail.com"
//            val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
//            val email = sharedPrefs.getString("user_email", "") ?: ""
//            val userId = sharedPrefs.getInt("user_id", 0)

            if (fullName.isNotEmpty() && dateOfBirthText.isNotEmpty() &&
                gender.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty()
            ) {
                val intent = Intent(this, RegisterInformationActivity::class.java)
                startActivity(intent)

                // Save data to SharedPreferences
                val userRegister = getSharedPreferences("UserRegister", Context.MODE_PRIVATE)
                val editor = userRegister.edit()
                editor.putString("name", fullName)
                editor.putString("email", email)
                editor.putString("gender", gender)
                editor.putString("birth", dateOfBirthText)
                editor.putString("password", password)
                editor.apply()

                Log.d("RegisterActivity", "Button clicked!")

                finish()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    private fun registerUser(customer: User, callback: (Response, String?) -> Unit) {
//        GlobalScope.launch {
//            try {
//                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
//                val requestBody = jacksonObjectMapper().writeValueAsString(customer)
//                    .toRequestBody(jsonMediaType)
//
//
//                val request = Request.Builder()
//                    .url("http://mealplanner.aqgxexddffeza6gn.australiaeast.azurecontainer.io/api/v1/account/signup")
//                    .post(requestBody)
//                    .build()
//
//
//                client.newCall(request).enqueue(object : Callback {
//                    override fun onResponse(call: Call, response: Response) {
//                        val responseBody = response.body?.string()
//                        callback(response, responseBody)
//                    }
//
//                    override fun onFailure(call: Call, e: IOException) {
//                        // Handle network failure
////                        callback(Response.Builder().code(-1).build(), e.message)
//                    }
//                })
//
//
//            } catch (e: Exception) {
//                // Handle other exceptions
////                callback(Response.Builder().code(-1).build(), e.message)
//            }
//        }
//
//    }

    private fun createMealPref(customer: User, callback: (Response, String?) -> Unit) {
        GlobalScope.launch {
            try {
                val requestBodyCreateMealPref = JSONObject()
//                requestBodyCreateMealPref.put("user_id", customer.user_id)
//                requestBodyCreateMealPref.put("pref_calo", customer.user_tdee)
                requestBodyCreateMealPref.put("pref_time", 60)
                requestBodyCreateMealPref.put("pref_goal", 1)
                requestBodyCreateMealPref.put("pref_date_range", 1)

                val requestCreateMealPref =
                    Request.Builder().url("http://10.0.2.2:8001/apis/mealpref/create")
                        .post(
                            requestBodyCreateMealPref.toString()
                                .toRequestBody("application/json".toMediaTypeOrNull())
                        )
                        .build()

                client.newCall(requestCreateMealPref).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string()
                        callback(response, responseBody)
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        // Handle network failure
//                        callback(Response.Builder().code(-1).build(), e.message)
                    }
                })

            } catch (e: Exception){
        }
    }}

    private fun fetchUserProfile(userId: Int, callback: (User?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val requestBody = JSONObject()
                requestBody.put("user_id", userId)

                val request = Request.Builder()
                    .url("http://10.0.2.2:8001/apis/user/get")
                    .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody.toString()))
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

                if (!response.isSuccessful) {
                    throw IOException("Failed to retrieve user profile")
                }

                val responseBody = response.body?.string()
                val resultJson = JSONObject(responseBody ?: "")
                val status = resultJson.optInt("status", 0)

                if (status == 1) {
                    val data = resultJson.optJSONArray("data")
                    val user = data?.optJSONObject(0)
                    val userObj = jacksonObjectMapper().readValue(user?.toString() ?: "", User::class.java)
                    callback(userObj)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                // Handle the error here
                callback(null)
                Log.e("UserProfile", "Failed to retrieve user profile: ${e.message}")
            }
        }
    }
}
