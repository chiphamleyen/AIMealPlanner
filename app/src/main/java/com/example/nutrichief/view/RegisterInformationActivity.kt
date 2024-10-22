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
import java.util.Date

class RegisterInformationActivity : AppCompatActivity() {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

//    private var actLevelInt: Int? = null
//    private lateinit var genderList: Spinner
//    private var selectedGender: String? = null
//    private val genders = arrayOf("Male", "Female")
//
//    private lateinit var activeLevelList: Spinner
//    private var selectedActiveLevel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_information)

        val registerBtn = findViewById<Button>(R.id.register_emp_btn)
//
//        genderList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                selectedGender = parent?.getItemAtPosition(position) as? String
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                selectedGender = null
//            }
//        }
//
//        // Create an ArrayAdapter using the string array and a default spinner layout
//        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
//
//        // Specify the layout to use when the list of choices appears
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//
//        // Apply the adapter to the spinner
//        genderList.adapter = adapter
//
//        activeLevelList = findViewById(R.id.activeLevel)
//        activeLevelList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                selectedActiveLevel = parent?.getItemAtPosition(position) as? String
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                selectedActiveLevel = null
//            }
//        }
//
//        // Create an ArrayAdapter using the string array and a default spinner layout
//        ArrayAdapter.createFromResource(
//            this,
//            R.array.activeLevel_array, // Replace with your own array resource name
//            android.R.layout.simple_spinner_item
//        ).also { adapter ->
//            // Specify the layout to use when the list of choices appears
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//            // Apply the adapter to the spinner
//            activeLevelList.adapter = adapter
//        }
//
//
        registerBtn.setOnClickListener {
            val userRegister = getSharedPreferences("UserRegister", Context.MODE_PRIVATE)
            val name = userRegister.getString("name", "") ?: ""
            val email = userRegister.getString("email", "") ?: ""
            val gender = userRegister.getString("gender", "") ?: ""
            val birth = userRegister.getString("birth", "") ?: ""
            val password = userRegister.getString("password", "") ?: ""

            val weightText = findViewById<TextInputEditText>(R.id.weight).text.toString()
            val heightText = findViewById<TextInputEditText>(R.id.height).text.toString()

            val allergyInput = findViewById<TextInputEditText>(R.id.allergy).text.toString()
            val allergyList = allergyInput.split(",").map { it.trim() }
            for (allergy in allergyList) {
                Log.d("Allergy", allergy)
            }

            val dietInput = findViewById<TextInputEditText>(R.id.diet_pref).text.toString()
            val dietList = dietInput.split(",").map { it.trim() }

            val date = LocalDate.parse(birth)
            val zonedDateTime = date.atStartOfDay(ZoneId.of("UTC"))
            val instant = zonedDateTime.toInstant()
            val dateUtil: Date = Date.from(instant)

            if (weightText.isNotEmpty() && heightText.isNotEmpty()) {
                val height = try {
                    heightText.toFloatOrNull() ?: 0.0f
                } catch (e: NumberFormatException) {
                    return@setOnClickListener
                }

                val weight = try {
                    weightText.toFloatOrNull() ?: 0.0f
                } catch (e: NumberFormatException) {
                    return@setOnClickListener
                }

                val user = User(
                    name,
                    email,
                    password,
                    gender,
                    height,
                    weight,
                    dateUtil,
                    allergyList,
                    dietList,
                    "ID1"
                )

                Log.d("USER", user.toString())

                GlobalScope.launch(Dispatchers.IO) {
                    registerUser(user) { response, errorMessage ->
                        Log.e("confirm", "clicked")
                        runOnUiThread {
                            if (response.isSuccessful) {
                                // Registration successful
                                Toast.makeText(
                                    this@RegisterInformationActivity,
                                    "Registration successful",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                val loginIntent = Intent(this@RegisterInformationActivity, LoginActivity::class.java)
                                startActivity(loginIntent)

//                                fetchUserProfile(userId) { user ->
//                                    user?.let {
//                                        createMealPref(it) { response, errorMessage ->
//                                            if (response.isSuccessful) {
//                                                val loginIntent =
//                                                    Intent(
//                                                        this@RegisterInformationActivity,
//                                                        MainActivity::class.java
//                                                    )
//                                                val sharedPrefs =
//                                                    getSharedPreferences(
//                                                        "MyPrefs",
//                                                        Context.MODE_PRIVATE
//                                                    )
//                                                val editor = sharedPrefs.edit()
//                                                editor.putString("user_name", fullName)
//                                                startActivity(loginIntent)
//                                                finish()
//                                            } else {
//                                                // Registration failed
//                                                if (errorMessage != null) {
//                                                    Toast.makeText(
//                                                        this@RegisterInformationActivity,
//                                                        errorMessage,
//                                                        Toast.LENGTH_SHORT
//                                                    ).show()
//                                                    Log.e("Meal Pref", errorMessage)
//                                                } else {
//                                                    Toast.makeText(
//                                                        this@RegisterInformationActivity,
//                                                        "Failed to create meal pref",
//                                                        Toast.LENGTH_SHORT
//                                                    )
//                                                        .show()
//                                                }
//                                            }
//                                        }
//                                    } ?: run {
//                                        // Handle the case when user is null (error occurred)
//                                        Toast.makeText(
//                                            this@RegisterInformationActivity,
//                                            "Failed to retrieve user profile",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                        Log.e("UserProfile", "Failed to retrieve user profile")
//                                    }
//                                }
                            } else {
                                // Registration failed
                                if (errorMessage != null) {
                                    Toast.makeText(
                                        this@RegisterInformationActivity,
                                        errorMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Log.d("error", errorMessage)
                                } else {
                                    Toast.makeText(
                                        this@RegisterInformationActivity,
                                        "Failed to register user",
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

    private fun registerUser(customer: User, callback: (Response, String?) -> Unit) {
        GlobalScope.launch {
            try {
                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jacksonObjectMapper().writeValueAsString(customer)
                    .toRequestBody(jsonMediaType)


                val request = Request.Builder()
                    .url("http://mealplanner2.f5cda3hmgmgbb7ba.australiaeast.azurecontainer.io/api/v1/account/signup")
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

//    private fun createMealPref(customer: User, callback: (Response, String?) -> Unit) {
//        GlobalScope.launch {
//            try {
//                val requestBodyCreateMealPref = JSONObject()
//                requestBodyCreateMealPref.put("user_id", customer.user_id)
//                requestBodyCreateMealPref.put("pref_calo", customer.user_tdee)
//                requestBodyCreateMealPref.put("pref_time", 60)
//                requestBodyCreateMealPref.put("pref_goal", 1)
//                requestBodyCreateMealPref.put("pref_date_range", 1)
//
//                val requestCreateMealPref =
//                    Request.Builder().url("http://10.0.2.2:8001/apis/mealpref/create")
//                        .post(
//                            requestBodyCreateMealPref.toString()
//                                .toRequestBody("application/json".toMediaTypeOrNull())
//                        )
//                        .build()
//
//                client.newCall(requestCreateMealPref).enqueue(object : Callback {
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
//            } catch (e: Exception){
//        }
//    }}
//
//    private fun fetchUserProfile(userId: Int, callback: (User?) -> Unit) {
//        GlobalScope.launch(Dispatchers.Main) {
//            try {
//                val requestBody = JSONObject()
//                requestBody.put("user_id", userId)
//
//                val request = Request.Builder()
//                    .url("http://10.0.2.2:8001/apis/user/get")
//                    .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody.toString()))
//                    .build()
//
//                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
//
//                if (!response.isSuccessful) {
//                    throw IOException("Failed to retrieve user profile")
//                }
//
//                val responseBody = response.body?.string()
//                val resultJson = JSONObject(responseBody ?: "")
//                val status = resultJson.optInt("status", 0)
//
//                if (status == 1) {
//                    val data = resultJson.optJSONArray("data")
//                    val user = data?.optJSONObject(0)
//                    val userObj = jacksonObjectMapper().readValue(user?.toString() ?: "", User::class.java)
//                    callback(userObj)
//                } else {
//                    callback(null)
//                }
//            } catch (e: Exception) {
//                // Handle the error here
//                callback(null)
//                Log.e("UserProfile", "Failed to retrieve user profile: ${e.message}")
//            }
//        }
//    }
}
