package com.example.nutrichief.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.example.nutrichief.R
import com.example.nutrichief.datamodels.User
import com.example.nutrichief.datamodels.UserProfileEdit
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class UserProfileSettingsActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private lateinit var genderList: Spinner
    private var selectedGender: String? = null
    private val genders = arrayOf("Male", "Female")

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_settings)

        val saveBtn = findViewById<Button>(R.id.save_emp_btn)
        genderList = findViewById(R.id.gender)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderList.adapter = adapter

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val jwtToken = sharedPreferences.getString("jwt_token", null)

        if (jwtToken != null) {
            Log.d("JWT Token", jwtToken)
        }

        // Load user profile
        fetchUserProfile(jwtToken) { user ->
            user?.let {
                // Populate user profile data to TextViews
                findViewById<TextInputEditText>(R.id.fullname).setText(it.name)
                findViewById<TextInputEditText>(R.id.yearofbirth).setText(formatDate(it.date_of_birth))
                genderList.setSelection(genders.indexOf(it.gender))
                findViewById<TextInputEditText>(R.id.height).setText(it.height.toString())
                findViewById<TextInputEditText>(R.id.weight).setText(it.weight.toString())
                findViewById<TextInputEditText>(R.id.allergy).setText(it.allergies?.joinToString(", ") ?: "")
                findViewById<TextInputEditText>(R.id.diet_pref).setText(it.dietary_preferences?.joinToString(", ") ?: "")

            } ?: run {
                // Handle the case when user is null (error occurred)
                Toast.makeText(this, "Failed to retrieve user profile", Toast.LENGTH_SHORT).show()
                Log.e("UserProfile", "Failed to retrieve user profile")
            }
        }


        saveBtn.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun formatDate(date: Date?): String {
        return if (date != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.format(date)
        } else {
            ""
        }
    }

    private fun fetchUserProfile(token: String?, callback: (User?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                if (token == null) {
                    callback(null)
                    return@launch
                }

                val request = Request.Builder()
                    .url("http://mealplanner2.f5cda3hmgmgbb7ba.australiaeast.azurecontainer.io/api/v1/account/profile")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

                if (!response.isSuccessful) {
                    throw IOException("Failed to retrieve user profile")
                }

                val responseBody = response.body?.string()
                val resultJson = JSONObject(responseBody ?: "")
                val errorCode = resultJson.optInt("error_code", -1)

                if (errorCode == 0) {
                    val data = resultJson.optJSONObject("data")

                    val userObj = jacksonObjectMapper().readValue(data?.toString() ?: "", User::class.java)

                    callback(userObj)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
                Log.e("UserProfile", "Failed to retrieve user profile: ${e.message}")
            }
        }
    }

    private fun saveUserProfile() {
        val fullName = findViewById<TextInputEditText>(R.id.fullname).text.toString()
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val jwtToken = sharedPreferences.getString("jwt_token", null)

        if (jwtToken != null) {
            Log.d("JWT Token", jwtToken)
        }

        val yearOfBirthText = findViewById<TextInputEditText>(R.id.yearofbirth).text.toString()
        val gender = selectedGender.toString()
        val weightText = findViewById<TextInputEditText>(R.id.weight).text.toString()
        val heightText = findViewById<TextInputEditText>(R.id.height).text.toString()

        val allergyInput = findViewById<TextInputEditText>(R.id.allergy).text.toString()
        val allergyList = allergyInput.split(",").map { it.trim() }

        val dietInput = findViewById<TextInputEditText>(R.id.diet_pref).text.toString()
        val dietList = dietInput.split(",").map { it.trim() }

        // Check if required fields are filled
        if (fullName.isNotEmpty() && yearOfBirthText.isNotEmpty() &&
            weightText.isNotEmpty() && heightText.isNotEmpty()
        ) {
            val height = heightText.toFloatOrNull() ?: return
            val weight = weightText.toFloatOrNull() ?: return

            val date = LocalDate.parse(yearOfBirthText)
            val zonedDateTime = date.atStartOfDay(ZoneId.of("UTC"))
            val instant = zonedDateTime.toInstant()
            val dateUtil: Date = Date.from(instant)

            // Create a User object
            val user = UserProfileEdit(
                fullName,
                gender,
                weight,
                height,
                dateUtil,
                allergyList,
                dietList,
                "ID1"
            )

            // Update user in background thread
            GlobalScope.launch(Dispatchers.IO) {
                updateUser(jwtToken, user) { response, errorMessage ->
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(this@UserProfileSettingsActivity, "Update Profile Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@UserProfileSettingsActivity, UserProfileActivity::class.java))
                            finish()
                        } else {
                            val message = errorMessage ?: "Failed to register user"
                            Toast.makeText(this@UserProfileSettingsActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUser(token: String?, customer: UserProfileEdit, callback: (Response, String?) -> Unit) {
        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jacksonObjectMapper().writeValueAsString(customer).toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("http://mealplanner.aqgxexddffeza6gn.australiaeast.azurecontainer.io/api/v1/account/edit")
                .addHeader("Authorization", "Bearer $token")
                .put(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    callback(response, responseBody)
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback(Response.Builder().code(-1).build(), e.message)
                }
            })
        } catch (e: Exception) {
            callback(Response.Builder().code(-1).build(), e.message)
        }
    }

}
