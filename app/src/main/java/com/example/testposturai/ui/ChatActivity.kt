package com.example.testposturai.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.testposturai.BuildConfig
import com.example.testposturai.chat.ChatAdapter
import com.example.testposturai.chat.ChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var input: EditText
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(130, TimeUnit.SECONDS)
        .build()
    @Volatile
    private var isClosed = false

    private val apiBaseUrl = BuildConfig.CHAT_API_BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleScreen(root)

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnBack = Button(this).apply {
            text = "Tornar"
            setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        UiKit.styleSecondaryButton(btnBack)

        val title = TextView(this).apply {
            text = "Assistent de postura"
            setPadding(UiKit.dp(this@ChatActivity, 12), 0, 0, 0)
        }
        UiKit.styleTitle(title)
        title.textSize = 20f

        topBar.addView(btnBack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f))
        topBar.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f))

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            background = UiKit.roundedDrawable(UiKit.colorSurface, UiKit.colorBorder, 1, 16f)
            setPadding(UiKit.dp(this@ChatActivity, 8), UiKit.dp(this@ChatActivity, 8), UiKit.dp(this@ChatActivity, 8), UiKit.dp(this@ChatActivity, 8))
        }
        adapter = ChatAdapter(messages)
        recyclerView.adapter = adapter

        input = EditText(this).apply { hint = "Escriu una pregunta..." }
        UiKit.styleInput(input)

        val btnSend = Button(this).apply {
            text = "Enviar"
            setOnClickListener {
                val question = input.text.toString().trim()
                if (question.isNotEmpty()) {
                    sendQuestion(question)
                    input.text.clear()
                }
            }
        }
        UiKit.stylePrimaryButton(btnSend)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend)
            (btnSend.layoutParams as LinearLayout.LayoutParams).marginStart = UiKit.dp(this@ChatActivity, 10)
        }

        root.addView(topBar)
        root.addView(recyclerView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        (recyclerView.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 14)
        root.addView(inputRow)
        (inputRow.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 12)

        setContentView(root)
    }

    private fun sendQuestion(question: String) {
        addMessage(ChatMessage(question, isUser = true))

        val payload = JSONObject().apply {
            put("question", question)
        }

        val request = Request.Builder()
            .url("$apiBaseUrl/chat")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (isClosed) return@use
                    val backendSignature = response.header("X-PosturAI-Backend", "")
                    if (backendSignature != "true") {
                        runOnUiThread {
                            if (!isClosed) {
                                addMessage(
                                    ChatMessage(
                                        "Error de configuració: el xat està connectat a un backend incorrecte. Revisa la URL d'ngrok.",
                                        isUser = false
                                    )
                                )
                            }
                        }
                        return@use
                    }
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string().orEmpty()
                        val backendMessage = try {
                            JSONObject(errorBody).optString("error", "")
                        } catch (_: Exception) {
                            ""
                        }
                        runOnUiThread {
                            if (!isClosed) {
                                val msg = if (backendMessage.isNotBlank()) {
                                    "Error backend ${response.code}: $backendMessage"
                                } else {
                                    "Error backend: ${response.code}"
                                }
                                addMessage(ChatMessage(msg, isUser = false))
                            }
                        }
                        return@use
                    }

                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val answer = json.optString("answer", "Sense resposta")

                    runOnUiThread {
                        if (!isClosed) addMessage(ChatMessage(answer, isUser = false))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isClosed) {
                        Toast.makeText(applicationContext, "Error de connexio: ${e.message}", Toast.LENGTH_LONG).show()
                        addMessage(ChatMessage("No he pogut connectar amb la IA.", isUser = false))
                    }
                }
            }
        }.start()
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onDestroy() {
        isClosed = true
        super.onDestroy()
    }
}
