package com.example.nutrichief.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nutrichief.R
import com.example.nutrichief.adapter.IngredientAdapter
import com.example.nutrichief.adapter.IngredientsAdapter
import com.example.nutrichief.datamodels.Food
import com.example.nutrichief.datamodels.Ingredient
import com.example.nutrichief.datamodels.Meal
import com.example.nutrichief.datamodels.RecipeIngredient
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var ingredientRecyclerView: RecyclerView
    private lateinit var adapter: IngredientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        ingredientRecyclerView = findViewById(R.id.ingredients_recycler_view)

        val mealTitle = intent.getStringExtra("meal_title")
        val mealCalories = intent.getDoubleExtra("meal_calories", 0.0)
        val mealProtein = intent.getDoubleExtra("meal_protein", 0.0)
        val mealFat = intent.getDoubleExtra("meal_fat", 0.0)
        val mealIngredients = intent.getStringArrayListExtra("meal_ingredients")
        val mealDirections = intent.getStringArrayListExtra("meal_directions")

        val titleTV = findViewById<TextView>(R.id.foodName)
        val kcalTV = findViewById<TextView>(R.id.caloriesValue)
        val proteinTV = findViewById<TextView>(R.id.proteinValue)
        val fatTV = findViewById<TextView>(R.id.fatValue)

        titleTV.text = mealTitle
        kcalTV.text = mealCalories.toString()
        proteinTV.text = mealProtein.toString()
        fatTV.text = mealFat.toString()

        mealIngredients?.let {
            adapter = IngredientsAdapter(it) // Assuming you have an adapter ready for the ingredient list
            ingredientRecyclerView.layoutManager = LinearLayoutManager(this)
            ingredientRecyclerView.adapter = adapter
        }

        val startCookingButton = findViewById<Button>(R.id.startCookingButton)
        startCookingButton.setOnClickListener {
            val intent = Intent(this, InstructionsActivity::class.java)
            intent.putStringArrayListExtra("meal_directions", mealDirections)
            startActivity(intent)
        }

        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}