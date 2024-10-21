package com.example.nutrichief.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.nutrichief.R
import com.example.nutrichief.datamodels.Ingredient
import com.example.nutrichief.datamodels.RecipeIngredient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException

class InstructionsActivity : AppCompatActivity() {
    private var currentPage = 1
    private var totalPages = 0

    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var stepNumber: TextView
    private lateinit var recipeTitle: TextView
    private lateinit var recipeQty: TextView
    private lateinit var recipeDesc: TextView
    private var videoPath: String? = null
    private lateinit var stepContainer: LinearLayout
    private lateinit var directionsList: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        stepNumber = findViewById(R.id.cooking_step)
        recipeDesc = findViewById(R.id.recipeDesc)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)

        stepContainer = findViewById(R.id.buttonContainer)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val mediaController = MediaController(this)

        if (videoPath.isNullOrEmpty()) {
            videoPath = "https://www.shutterstock.com/shutterstock/videos/1009023404/preview/stock-footage-rapidly-chopping-onion-close-up-slow-mothion-red-onions-close-up-female-hands-cut-onions-in.webm"
        }

        if (!videoPath.isNullOrEmpty()) {
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            videoView.setVideoURI(Uri.parse(videoPath))

//            videoView.requestFocus()
//            videoView.start()
        } else {
            Log.e("InstructionsActivity", "Video path is null or empty")
        }


        val directions = intent.getStringArrayListExtra("meal_directions")

        directions?.let {
            directionsList = it
            totalPages = directionsList.size
            createStepButtons(totalPages)

            updateInstruction()
            highlightCurrentStep(currentPage)

            previousButton.setOnClickListener {
                if (currentPage > 1) {
                    currentPage--
                    updateInstruction()
                    highlightCurrentStep(currentPage)
                }
            }

            nextButton.setOnClickListener {
                if (currentPage < totalPages) {
                    currentPage++
                    updateInstruction()
                    highlightCurrentStep(currentPage)
                }
            }
        }
    }

    private fun updateInstruction() {
        if (currentPage <= directionsList.size) {
            recipeDesc.text = directionsList[currentPage - 1]
            updateButtonVisibility()
        }
    }

    private fun createStepButtons(totalSteps: Int) {
        stepContainer.gravity = Gravity.CENTER
        for (i in 1..totalSteps) {
            val stepButton = MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    setMargins(10, 0, 10, 0)
                }
                text = i.toString()
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.BLACK)
                cornerRadius = 28
                setOnClickListener {
                    currentPage = i
                    updateInstruction()
                    highlightCurrentStep(currentPage)
                }
            }

            stepContainer.addView(stepButton)
        }
    }

    private fun highlightCurrentStep(currentStep: Int) {
        for (i in 0 until stepContainer.childCount) {
            val stepButton = stepContainer.getChildAt(i) as MaterialButton
            val themeColor = ContextCompat.getColor(this, R.color.purple_theme_color)
            if (i == currentStep - 1) {
                stepButton.setBackgroundColor(themeColor)
                stepButton.setTextColor(Color.WHITE)
            } else {
                stepButton.setBackgroundColor(Color.WHITE)
                stepButton.setTextColor(Color.BLACK)
            }
        }
    }

    private fun updateButtonVisibility() {
        previousButton.visibility = if (currentPage == 1) View.GONE else View.VISIBLE
        nextButton.visibility = if (currentPage == totalPages) View.GONE else View.VISIBLE
//        if (currentPage == totalPages) {
//            nextButton.text = "Finish"
//            if (nextButton.text == "Finish")
//                nextButton.setOnClickListener { finish() }
//        }
//        else nextButton.text = "Next step"
    }
}

// LOOP STEPS CIRCLES
//override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    setContentView(R.layout.activity_layout)
//
//    val buttonContainer: LinearLayout = findViewById(R.id.buttonContainer)
//
//    // Replace this with your actual logic to retrieve the number from the database
//    val numberOfButtonsFromDatabase = retrieveNumberOfButtonsFromDatabase()
//
//    for (i in 0 until numberOfButtonsFromDatabase) {
//        val button = Button(this)
//        button.text = "Button ${i + 1}"
//        button.setOnClickListener {
//            // Handle button click here
//        }
//        buttonContainer.addView(button)
//    }
//}


// FULLSCREEN
// create layout
//<?xml version="1.0" encoding="utf-8"?>
//<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
//xmlns:app="http://schemas.android.com/apk/res-auto"
//xmlns:tools="http://schemas.android.com/tools"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//tools:context=".FullscreenActivity">
//
//<VideoView
//android:id="@+id/fullscreenVideoView"
//android:layout_width="match_parent"
//android:layout_height="match_parent" />
//
//<!-- You can add any other UI elements here, such as close button or title bar -->
//
//</RelativeLayout>

// NEW ACTIVITY
//import android.net.Uri
//import android.os.Bundle
//import android.widget.MediaController
//import android.widget.VideoView
//import androidx.appcompat.app.AppCompatActivity
//
//class FullscreenActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_fullscreen)
//
//        val videoView = findViewById<VideoView>(R.id.fullscreenVideoView)
//        val videoUrl = intent.getStringExtra("videoUrl")
//
//        val mediaController = MediaController(this)
//        mediaController.setAnchorView(videoView)
//        videoView.setMediaController(mediaController)
//        videoView.setVideoURI(Uri.parse(videoUrl))
//        videoView.start()
//    }
//}

// modify current
//val videoView = findViewById<VideoView>(R.id.videoView)
//val videoUrl = "https://www.example.com/path/to/your/video.mp4"
//
//videoView.setOnClickListener {
//    val intent = Intent(this, FullscreenActivity::class.java)
//    intent.putExtra("videoUrl", videoUrl)
//    startActivity(intent)
//}

// CLOSE BUTTON
// modify full screen.xml
//<?xml version="1.0" encoding="utf-8"?>
//<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
//xmlns:app="http://schemas.android.com/apk/res-auto"
//xmlns:tools="http://schemas.android.com/tools"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//tools:context=".FullscreenActivity">
//
//<VideoView
//android:id="@+id/fullscreenVideoView"
//android:layout_width="match_parent"
//android:layout_height="match_parent" />
//
//<ImageButton
//android:id="@+id/closeButton"
//android:layout_width="wrap_content"
//android:layout_height="wrap_content"
//android:layout_alignParentEnd="true"
//android:layout_margin="16dp"
//android:background="?android:attr/selectableItemBackgroundBorderless"
//android:src="@drawable/ic_close" />
//
//</RelativeLayout>

// modify activity
//import android.net.Uri
//import android.os.Bundle
//import android.widget.ImageButton
//import android.widget.MediaController
//import android.widget.VideoView
//import androidx.appcompat.app.AppCompatActivity
//
//class FullscreenActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_fullscreen)
//
//        val videoView = findViewById<VideoView>(R.id.fullscreenVideoView)
//        val closeButton = findViewById<ImageButton>(R.id.closeButton)
//        val videoUrl = intent.getStringExtra("videoUrl")
//
//        closeButton.setOnClickListener {
//            finish() // Close the activity when the close button is clicked
//        }
//
//        val mediaController = MediaController(this)
//        mediaController.setAnchorView(videoView)
//        videoView.setMediaController(mediaController)
//        videoView.setVideoURI(Uri.parse(videoUrl))
//        videoView.start()
//    }
//}
