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
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.nutrichief.datamodels.Meal
import com.example.nutrichief.view.RecipeDetailActivity
import com.example.nutrichief.view.UserProfileActivity
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

    // Constants for SharedPreferences keys
    private val MEAL_PLAN_KEY = "meal_plan"
    private val LAST_FETCH_DATE_KEY = "last_fetch_date"

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

        val sharedPreferences = activity?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) ?: return view

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
                saveMealPlanToLocal(sharedPreferences, meals) // Lưu meal mới vào SharedPreferences
                updateMealPlan(meals) // Cập nhật UI với meal mới
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

    private fun getGeneratedMealPlan(
        token: String?,
        resultHandleFunction: (List<Meal>?) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val requestBody = JSONObject()

                val request = Request.Builder()
                    .url("http://mealplanner.aqgxexddffeza6gn.australiaeast.azurecontainer.io/api/v1/suggestions/without_llm")
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