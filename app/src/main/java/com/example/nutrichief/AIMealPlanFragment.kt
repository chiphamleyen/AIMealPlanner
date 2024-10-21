package com.example.nutrichief

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentTransaction
import com.example.nutrichief.datamodels.Meal
import com.example.nutrichief.view.RecipeDetailActivity
import com.example.nutrichief.view.UserProfileActivity
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_MEAL_LIST = "meal_list"

/**
 * A simple [Fragment] subclass.
 * Use the [AIMealPlanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AIMealPlanFragment : Fragment() {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private var mealList = mutableListOf<Meal>()

    private lateinit var mealTitle1: TextView
    private lateinit var mealTitle2: TextView
    private lateinit var mealTitle3: TextView

    private lateinit var regenerateButton: Button

    private lateinit var mealCard1: CardView
    private lateinit var mealCard2: CardView
    private lateinit var mealCard3: CardView

    //init checkbox and streak variables
    private lateinit var streakTextView: TextView
    private lateinit var checkbox1: CheckBox
    private lateinit var checkbox2: CheckBox
    private lateinit var checkbox3: CheckBox

    // chatbox
    private lateinit var chatbox: LinearLayout
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageButton

    // Constants for SharedPreferences keys
    private val MEAL_PLAN_KEY = "meal_plan"
    private val LAST_FETCH_DATE_KEY = "last_fetch_date"
    private val STREAK_COUNTER_KEY = "streak_counter"
    private val LAST_CHECKED_TIME_KEY = "last_checked_time"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            mealList = if (Build.VERSION.SDK_INT >= 33) {
//                it.getParcelableArrayList(ARG_MEAL_LIST, Meal::class.java) as MutableList<Meal>
//            } else {
//                it.getParcelableArrayList<Meal>(ARG_MEAL_LIST) as MutableList<Meal>
//            }
//        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_meal_plan, container, false)

        mealTitle1 = view.findViewById(R.id.meal_title_1)
        mealTitle2 = view.findViewById(R.id.meal_title_2)
        mealTitle3 = view.findViewById(R.id.meal_title_3)
        regenerateButton = view.findViewById(R.id.regenerate_button)
        mealCard1 = view.findViewById(R.id.meal1)
        mealCard2 = view.findViewById(R.id.meal2)
        mealCard3 = view.findViewById(R.id.meal3)

        streakTextView = view.findViewById(R.id.streakTextView)

        checkbox1 = view.findViewById(R.id.checkbox_meal1)
        checkbox2 = view.findViewById(R.id.checkbox_meal2)
        checkbox3 = view.findViewById(R.id.checkbox_meal3)

        chatbox = view.findViewById(R.id.chatbox)

        val sharedPreferences = activity?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) ?: return view

        // Initialize checkbox listeners to update streak counter when checkboxes are toggled
        checkbox1.setOnCheckedChangeListener { _, _ -> updateStreakCounter(sharedPreferences) }
        checkbox2.setOnCheckedChangeListener { _, _ -> updateStreakCounter(sharedPreferences) }
        checkbox3.setOnCheckedChangeListener { _, _ -> updateStreakCounter(sharedPreferences) }

        // Update streak TextView when the fragment is created to display the current streak
        updateStreakTextView(sharedPreferences)

        editText = view.findViewById(R.id.chatbox_edittext)
        sendButton = view.findViewById(R.id.chatbox_send_button)

        sendButton.setOnClickListener {
            val userMessage = editText.text.toString()

            if (userMessage.isNotEmpty()) {
                // Create a bundle to pass the message
                val bundle = Bundle()
                bundle.putString("userMessage", userMessage)

                // Create an instance of ChatAIFragment
                val chatAIFragment = ChatAIFragment()
                chatAIFragment.arguments = bundle

                // Get the current fragment (AIMealPlanFragment)
                val currentFragment = parentFragmentManager.findFragmentById(R.id.fragment_container) as? AIMealPlanFragment

                if (currentFragment != null) {
                    // Hide AIMealPlanFragment
                    currentFragment.view?.visibility = View.GONE // Ẩn AIMealPlanFragment

                    // Add ChatAIFragment
                    parentFragmentManager.beginTransaction()
                        .add(R.id.fragment_container, chatAIFragment)
                        .addToBackStack(null) // Optional, if you want to navigate back
                        .commit()
                }
            }
        }


        // Check if the meal plan is up-to-date
        if (isMealPlanUpToDate(sharedPreferences)) {
            val savedMeals = loadMealPlanFromLocal(sharedPreferences)
            if (savedMeals != null) {
                updateMealPlan(savedMeals)
            }
        } else {
            // If no up-to-date meal plan, fetch from API and save to local storage
            val jwtToken = sharedPreferences.getString("jwt_token", null)
            getGeneratedMealPlan(jwtToken) { meals ->
                if (meals != null) {
                    saveMealPlanToLocal(sharedPreferences, meals)
                    updateMealPlan(meals)
                }
            }
        }

        regenerateButton.setOnClickListener {
            regenerateMeal(sharedPreferences)

        }

        val imageUserAva = view.findViewById<ImageView>(R.id.user_ava)
        imageUserAva.setOnClickListener {
            val intent = Intent(activity, UserProfileActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun updateMealPlan(meals: List<Meal>) {
        if (meals.size >= 3) {
            // Update the TextViews with meal titles
            mealTitle1.text = meals[0].title
            mealTitle2.text = meals[1].title
            mealTitle3.text = meals[2].title

            // Set onClickListener for each meal
            mealCard1.setOnClickListener {
                openRecipeDetailActivity(meals[0])
            }
            mealCard2.setOnClickListener {
                openRecipeDetailActivity(meals[1])
            }
            mealCard3.setOnClickListener {
                openRecipeDetailActivity(meals[2])
            }
        }
    }

    private fun openRecipeDetailActivity(meal: Meal) {
        val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
        intent.putExtra("meal_title", meal.title)
        intent.putExtra("meal_calories", meal.calories)
        intent.putExtra("meal_protein", meal.protein)
        intent.putExtra("meal_fat", meal.fat)
        intent.putExtra("meal_ingredients", ArrayList(meal.ingredients))
        intent.putExtra("meal_directions", ArrayList(meal.directions))
        startActivity(intent)
    }

    // Function to check if the stored meal plan is valid for today
    private fun isMealPlanUpToDate(sharedPreferences: SharedPreferences): Boolean {
        val lastFetchDate = sharedPreferences.getString(LAST_FETCH_DATE_KEY, null)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return lastFetchDate == currentDate
    }

    // Function to regenerate meal when button is clicked
    private fun regenerateMeal(sharedPreferences: SharedPreferences) {
        val jwtToken = sharedPreferences.getString("jwt_token", null)
        getGeneratedMealPlan(jwtToken) { meals ->
            if (meals != null) {
                saveMealPlanToLocal(sharedPreferences, meals)
                updateMealPlan(meals)
                Log.d("regenerated meal", meals.toString())
            }
        }
    }

    // Function to save meal plan and current date into SharedPreferences
    private fun saveMealPlanToLocal(sharedPreferences: SharedPreferences, meals: List<Meal>) {
        val editor = sharedPreferences.edit()

        // Convert the meal list to JSON string
        val mealJsonArray = JSONArray()
        meals.forEach { meal ->
            val mealJson = JSONObject()
            mealJson.put("title", meal.title)
            mealJson.put("ingredients", JSONArray(meal.ingredients))
            mealJson.put("directions", JSONArray(meal.directions))
            mealJson.put("calories", meal.calories)
            mealJson.put("fat", meal.fat)
            mealJson.put("protein", meal.protein)
            mealJson.put("sodium", meal.sodium)
            mealJson.put("rating", meal.rating)
            mealJson.put("categories", JSONArray(meal.categories))
            mealJsonArray.put(mealJson)
        }

        // Save meal plan and the current date
        editor.putString(MEAL_PLAN_KEY, mealJsonArray.toString())
        editor.putString(LAST_FETCH_DATE_KEY, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date()
        ))
        editor.apply()
    }

    // Function to load meal plan from SharedPreferences
    private fun loadMealPlanFromLocal(sharedPreferences: SharedPreferences): List<Meal>? {
        val mealJsonString = sharedPreferences.getString(MEAL_PLAN_KEY, null)

        if (!TextUtils.isEmpty(mealJsonString)) {
            val mealJsonArray = JSONArray(mealJsonString)
            val meals = mutableListOf<Meal>()
            for (i in 0 until mealJsonArray.length()) {
                val mealJson = mealJsonArray.getJSONObject(i)
                val meal = Meal(
                    title = mealJson.getString("title"),
                    ingredients = List(mealJson.getJSONArray("ingredients").length()) {
                        mealJson.getJSONArray("ingredients").getString(it)
                    },
                    directions = List(mealJson.getJSONArray("directions").length()) {
                        mealJson.getJSONArray("directions").getString(it)
                    },
                    calories = mealJson.optDouble("calories", 0.0),
                    fat = mealJson.optDouble("fat", 0.0),
                    protein = mealJson.optDouble("protein", 0.0),
                    sodium = mealJson.optDouble("sodium", 0.0),
                    rating = mealJson.optDouble("rating", 0.0),
                    categories = List(mealJson.getJSONArray("categories").length()) {
                        mealJson.getJSONArray("categories").getString(it)
                    }
                )
                meals.add(meal)
            }
            return meals
        }
        return null
    }
    private fun updateStreakCounter(sharedPreferences: SharedPreferences) {
        val editor = sharedPreferences.edit()

        // Get the last checked time and the current time
        val lastCheckedTime = sharedPreferences.getLong(LAST_CHECKED_TIME_KEY, 0L)
        val currentTime = System.currentTimeMillis()

        // Check if any checkboxes are checked
        val anyCheckboxChecked = checkbox1.isChecked || checkbox2.isChecked || checkbox3.isChecked

        // Calculate the time difference in hours
        val hoursSinceLastChecked = (currentTime - lastCheckedTime) / (1000 * 60 * 60)

        if (anyCheckboxChecked) {
            // Update streak if checked within 24 hours
            if (hoursSinceLastChecked < 24) {
                val currentStreak = sharedPreferences.getInt(STREAK_COUNTER_KEY, 0)
                editor.putInt(STREAK_COUNTER_KEY, currentStreak + 1)
            } else {
                // Reset streak if more than 24 hours have passed since last check
                editor.putInt(STREAK_COUNTER_KEY, 1)
            }
            // Update the last checked time to current time
            editor.putLong(LAST_CHECKED_TIME_KEY, currentTime)
        } else if (hoursSinceLastChecked >= 24) {
            // Reset streak if no checkbox is checked in the past 24 hours
            editor.putInt(STREAK_COUNTER_KEY, 0)
        }

        // Apply the changes and update the streak TextView
        editor.apply()
        updateStreakTextView(sharedPreferences)
    }

    @SuppressLint("SetTextI18n")
    private fun updateStreakTextView(sharedPreferences: SharedPreferences) {
        val streak = sharedPreferences.getInt(STREAK_COUNTER_KEY, 0)
        streakTextView.text = "⚡$streak"
    }

    private fun getGeneratedMealPlan(
        token: String?,
        resultHandleFunction: (List<Meal>?) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val request = Request.Builder()
                    .url("http://mealplanner2.f5cda3hmgmgbb7ba.australiaeast.azurecontainer.io/api/v1/suggestions/with_llm")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

                if (!response.isSuccessful) {
                    throw IOException("Cannot get suggested meal")
                }

                val responseBody = response.body?.string()
                val resultJson = JSONObject(responseBody ?: "")
                val errorCode = resultJson.optInt("error_code", -1)

                if (errorCode == 0) {
                    val dataObject = resultJson.optJSONObject("data") ?: return@launch resultHandleFunction(null)

                    val mealData = dataObject.optJSONArray("data") ?: return@launch resultHandleFunction(null)

                    val meals = mutableListOf<Meal>()
                    for (i in 0 until mealData.length()) {
                        val mealJson = mealData.getJSONObject(i)
                        val meal = Meal(
                            title = mealJson.optString("title"),
                            ingredients = mealJson.optJSONArray("ingredients")?.let { jsonArray ->
                                List(jsonArray.length()) { jsonArray.getString(it) }
                            } ?: emptyList(),
                            directions = mealJson.optJSONArray("directions")?.let { jsonArray ->
                                List(jsonArray.length()) { jsonArray.getString(it) }
                            } ?: emptyList(),
                            calories = mealJson.optDouble("calories", 0.0),
                            fat = mealJson.optDouble("fat", 0.0),
                            protein = mealJson.optDouble("protein", 0.0),
                            sodium = mealJson.optDouble("sodium", 0.0),
                            rating = mealJson.optDouble("rating", 0.0),
                            categories = mealJson.optJSONArray("categories")?.let { jsonArray ->
                                List(jsonArray.length()) { jsonArray.getString(it) }
                            } ?: emptyList()
                        )
                        meals.add(meal)
                    }

                    resultHandleFunction(meals)
                } else {
                    resultHandleFunction(null)
                }

            } catch (e: Exception) {
                resultHandleFunction(null)
                Log.e("AIMealPlanFragment", "Failed to retrieve meal plan: ${e.message}")
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AIMealPlanFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AIMealPlanFragment().apply {
//                arguments = Bundle().apply {
//                    putParcelableArrayList(ARG_MEAL_LIST, mealList)
//                }
            }
    }
}