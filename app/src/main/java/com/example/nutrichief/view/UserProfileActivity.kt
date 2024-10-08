package com.example.nutrichief.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nutrichief.R
import com.example.nutrichief.datamodels.User
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

class UserProfileActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val fullName = findViewById<TextView>(R.id.profile_name)
        val email = findViewById<TextView>(R.id.profile_email)
        val gender = findViewById<TextView>(R.id.profile_gender)
        val age = findViewById<TextView>(R.id.profile_age)
        val height = findViewById<TextView>(R.id.profile_height)
        val weight = findViewById<TextView>(R.id.profile_weight)
        val allergy = findViewById<TextView>(R.id.profile_allergy)
        val diet_pref = findViewById<TextView>(R.id.profile_diet_pref)
        val update = findViewById<ImageView>(R.id.update_profile)
        val currentOrderBtn = findViewById<ImageView>(R.id.current_order)

        update.setOnClickListener {
            startActivity(Intent(this, UserProfileSettingsActivity::class.java))
            finish()
        }

        currentOrderBtn.setOnClickListener {
            startActivity(Intent(this, CurrentOrderActivity::class.java))
            finish()
        }

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val jwtToken = sharedPreferences.getString("jwt_token", null)

        if (jwtToken != null) {
            Log.d("JWT Token", jwtToken) // Sử dụng JWT token ở đây
        }

        fetchUserProfile(jwtToken) { user ->
            user?.let {
                // Populate user profile data to TextViews
                fullName.text = it.name
                email.text = it.email
                gender.text = it.gender
                val ageValue = calculateAge(it.date_of_birth)
                age.text = "$ageValue years old"
                height.text = "${it.height} cm"
                weight.text = "${it.weight} kg"
                allergy.text = it.allergies?.joinToString(", ") ?: "No allergies"
                diet_pref.text = it.dietary_preferences?.joinToString(", ") ?: "No diet preferences"
            } ?: run {
                // Handle the case when user is null (error occurred)
                Toast.makeText(this, "Failed to retrieve user profile", Toast.LENGTH_SHORT).show()
                Log.e("UserProfile", "Failed to retrieve user profile")
            }
        }
    }

    fun goBack(view: View) {
        onBackPressed()
    }

    private fun fetchUserProfile(token: String?, callback: (User?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                if (token == null) {
                    callback(null)
                    return@launch
                }

                val request = Request.Builder()
                    .url("http://mealplanner.aqgxexddffeza6gn.australiaeast.azurecontainer.io/api/v1/account/profile")
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
                    // Lấy dữ liệu từ trường "data"
                    val data = resultJson.optJSONObject("data")

                    // Chuyển đổi JSON thành đối tượng User
                    val userObj = jacksonObjectMapper().readValue(data?.toString() ?: "", User::class.java)

                    callback(userObj)
                } else {
                    callback(null) // Trả về null nếu có lỗi xảy ra
                }
            } catch (e: Exception) {
                // Xử lý lỗi
                callback(null)
                Log.e("UserProfile", "Failed to retrieve user profile: ${e.message}")
            }
        }
    }

    fun calculateAge(dateOfBirth: Date?): Int {
        val birthDate = dateOfBirth?.toInstant()
            ?.atZone(ZoneId.systemDefault())
            ?.toLocalDate()
        val currentDate = LocalDate.now()
        return ChronoUnit.YEARS.between(birthDate, currentDate).toInt()
    }
}
