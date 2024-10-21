package com.example.nutrichief

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nutrichief.datamodels.Meal
import com.example.nutrichief.view.RecipeDetailActivity
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatAIFragment : Fragment() {
    private lateinit var chatInput: TextInputEditText
    private lateinit var chatContainer: LinearLayout
    private lateinit var sendButton: ImageButton
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_ai, container, false)

        // Initialize UI elements
        chatInput = view.findViewById(R.id.chat_input)
        chatContainer = view.findViewById(R.id.chat_container)
        sendButton = view.findViewById(R.id.send_button)

        // Get JWT token from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val jwtToken = sharedPreferences.getString("jwt_token", null)

        if (jwtToken != null) {
            Log.d("JWT Token", jwtToken)
            // Connect to WebSocket with authorization
            connectWebSocketWithAuth(jwtToken)
        } else {
            Log.e("WebSocket", "No JWT token found")
        }

        sendButton.setOnClickListener {
            sendMessage()
        }

        return view
    }

    private fun connectWebSocketWithAuth(jwtToken: String) {
        // Create the request with the Authorization header
        val request = Request.Builder()
            .url("ws://mealplanner2.f5cda3hmgmgbb7ba.australiaeast.azurecontainer.io/api/v1/chat/full_response")
            .addHeader("Authorization", "Bearer $jwtToken")  // Add Authorization header
            .build()

        // Open the WebSocket connection
        webSocket = client.newWebSocket(request, ChatWebSocketListener())
    }

    private fun sendMessage() {
        val userMessage = chatInput.text.toString()

        if (userMessage.isNotEmpty()) {
            // Send message to WebSocket
            webSocket.send(userMessage)

            // Add user message to the chat
            addChatMessage(userMessage, isUser = true)

            // Clear input field
            chatInput.text?.clear()
        }
    }

    private fun addChatMessage(message: String, isUser: Boolean) {
        val textView = TextView(context)
        textView.text = message
        textView.setTextColor(resources.getColor(android.R.color.black))

        // Set padding to create margin between text and background
        val padding = 20
        textView.setPadding(padding, padding, padding, padding)

        // Set layout gravity for user or AI message
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Add margins between messages
        params.setMargins(0, 8, 0, 8)

        if (isUser) {
            params.gravity = View.TEXT_ALIGNMENT_VIEW_START
            textView.setBackgroundResource(R.drawable.search_bar_background)
        } else {
            params.gravity = View.TEXT_ALIGNMENT_VIEW_END
            textView.setBackgroundResource(R.drawable.search_bar_background)
        }
        textView.layoutParams = params

        // Add the message to the chat container
        chatContainer.addView(textView)
    }


    inner class ChatWebSocketListener : WebSocketListener() {

        private val mealList = mutableListOf<Meal>()
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // Parse WebSocket message
            val jsonObject = JSONObject(text)
            val messageType = jsonObject.getString("messageType")

            activity?.runOnUiThread {
                when (messageType) {
                    "BOT_TEXT_MESSAGE" -> {
                        val message = jsonObject.getString("message")
                        addChatMessage(message, isUser = false)
                        Log.d("text", message.toString())
                    }
                    "BOT_DETAIL_MESSAGE" -> {
                        val message = jsonObject.getJSONObject("message")
                        handleBotDetailMessage(message)
                        Log.d("meal", message.toString())
                    }
                    "HISTORY" -> {
                        val messageHistory = jsonObject.getJSONArray("message")
                        handleHistoryMessages(messageHistory)
                        Log.d("history", messageHistory.length().toString())
                    }
                }
            }
        }

        private fun handleHistoryMessages(messageHistory: JSONArray) {
            for (i in 0 until 10) {
                val messageObj = messageHistory.getJSONObject(i)
                val messageText = messageObj.getString("message")
                val msgType = messageObj.getString("messageType")

                if (msgType == "USERMESSAGE") {
                    addChatMessage(messageText, isUser = true)  // Add user message
                } else if (msgType == "BOT_TEXT_MESSAGE") {
                    addChatMessage(messageText, isUser = false)  // Add bot message
                } else if (msgType == "BOT_DETAIL_MESSAGE") {
                    val detailedMessage = messageObj.getJSONObject("message")
                    handleBotDetailMessage(detailedMessage)  // Add detailed bot message
                }
            }
        }


        private fun handleBotDetailMessage(message: JSONObject) {
            val meal = Meal(
                title = message.getString("title"),
                ingredients = List(message.getJSONArray("ingredients").length()) {
                    message.getJSONArray("ingredients").getString(it)
                },
                directions = List(message.getJSONArray("directions").length()) {
                    message.getJSONArray("directions").getString(it)
                },
                calories = message.optDouble("calories", 0.0),
                fat = message.optDouble("fat", 0.0),
                protein = message.optDouble("protein", 0.0),
                sodium = message.optDouble("sodium", 0.0),
                rating = message.optDouble("rating", 0.0),
                categories = List(message.getJSONArray("categories").length()) {
                    message.getJSONArray("categories").getString(it)
                }
            )

            mealList.add(meal)

            if (mealList.size >= 3) {
                displayMealCards(mealList)
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

        private fun displayMealCards(meals: List<Meal>) {
//            chatContainer.removeAllViews()

            for (meal in meals) {
                Log.d("meal display", meal.toString())
                val mealCardView = LayoutInflater.from(context).inflate(R.layout.meal_card, chatContainer, false)

                val mealTitle = mealCardView.findViewById<TextView>(R.id.meal_title)
                val mealDescription = mealCardView.findViewById<TextView>(R.id.meal_description)
                val detailsButton = mealCardView.findViewById<Button>(R.id.meal_details_button)

                val addMealButton = mealCardView.findViewById<ImageView>(R.id.add_meal_button)
                addMealButton.setOnClickListener {
                    saveMealToSharedPrefs(meal)
                }

                mealTitle.text = meal.title
                mealDescription.text = meal.rating.toString()

                detailsButton.setOnClickListener {
                    openRecipeDetailActivity(meal)
                }

                chatContainer.addView(mealCardView)
            }
        }

        private fun saveMealToSharedPrefs(meal: Meal) {
            val sharedPreferences = requireContext().getSharedPreferences("MealPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // Lấy danh sách meal đã lưu trước đó
            val mealListJson = sharedPreferences.getString("saved_meals", "[]")
            val mealList = JSONArray(mealListJson)

            // Chuyển meal thành JSONObject
            val mealJson = JSONObject().apply {
                put("title", meal.title)
                put("ingredients", JSONArray(meal.ingredients))
                put("directions", JSONArray(meal.directions))
                put("calories", meal.calories)
                put("fat", meal.fat)
                put("protein", meal.protein)
                put("sodium", meal.sodium)
                put("rating", meal.rating)
                put("categories", JSONArray(meal.categories))
            }

            // Thêm meal mới vào danh sách
            mealList.put(mealJson)

            // Lưu lại danh sách vào SharedPreferences
            editor.putString("saved_meals", mealList.toString())
            editor.apply()

            Toast.makeText(requireContext(), "Meal added!", Toast.LENGTH_SHORT).show()
        }


        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket", "Error: ${t.message}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            Log.d("WebSocket", "Closed: $reason")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Fragment destroyed")
    }
}
