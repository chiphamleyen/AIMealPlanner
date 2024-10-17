package com.example.nutrichief

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
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
            .url("ws://mealplanner.aqgxexddffeza6gn.australiaeast.azurecontainer.io/api/v1/chat/full_response")
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

        // Set layout gravity for user or AI message
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        if (isUser) {
            params.gravity = View.TEXT_ALIGNMENT_VIEW_END
            textView.setBackgroundResource(R.drawable.search_bar_background)
        } else {
            params.gravity = View.TEXT_ALIGNMENT_VIEW_START
            textView.setBackgroundResource(R.drawable.search_bar_background)
        }
        textView.layoutParams = params

        // Add the message to the chat container
        chatContainer.addView(textView)
    }

    inner class ChatWebSocketListener : WebSocketListener() {
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
                    }
                    "BOT_DETAIL_MESSAGE" -> {
                        val message = jsonObject.getJSONObject("message")
                        val title = message.getString("title")
                        val description = message.getString("desc")
                        val ingredients = message.getJSONArray("ingredients")
                        val directions = message.getJSONArray("directions")

                        // Display detailed message (you can customize the format)
                        val detailedMessage = StringBuilder()
                        detailedMessage.append("**$title**\n")
                        detailedMessage.append("$description\n")
                        detailedMessage.append("Ingredients:\n")
                        for (i in 0 until ingredients.length()) {
                            detailedMessage.append("- ${ingredients.getString(i)}\n")
                        }
                        detailedMessage.append("Directions:\n")
                        for (i in 0 until directions.length()) {
                            detailedMessage.append("- ${directions.getString(i)}\n")
                        }

                        addChatMessage(detailedMessage.toString(), isUser = false)
                    }
                }
            }
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
