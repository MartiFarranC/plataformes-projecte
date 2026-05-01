package com.example.testposturai.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

class ChatActivity : AppCompatActivity(), android.hardware.SensorEventListener {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var input: EditText
    private val client = OkHttpClient()

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val llindarCop = 10.5f // Ajustar segons força

    // Configurat a app/build.gradle.kts -> CHAT_API_BASE_URL
    private val apiBaseUrl = BuildConfig.CHAT_API_BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val btnBack = Button(this).apply {
            text = "TORNAR"
            setOnClickListener { finish() }
        }

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
        }
        adapter = ChatAdapter(messages)
        recyclerView.adapter = adapter

        input = EditText(this).apply {
            hint = "Escriu una pregunta..."
        }

        val btnSend = Button(this).apply {
            text = "ENVIAR"
            setOnClickListener {
                val question = input.text.toString().trim()
                if (question.isNotEmpty()) {
                    sendQuestion(question)
                    input.text.clear()
                }
            }
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(btnSend)
        }

        root.addView(btnBack)
        root.addView(recyclerView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(inputRow)

        setContentView(root)
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        if (event.sensor.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Magnitud vector: sqrt(x² + y² + z²)[cite: 3]
            val magnitud = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()

            if (magnitud > llindarCop) {
                Log.d("XAT_IA", "COP DETECTAT! Valor: $magnitud")
            }
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
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
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            addMessage(ChatMessage("Error backend: ${response.code}", isUser = false))
                        }
                        return@use
                    }

                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val answer = json.optString("answer", "Sense resposta")

                    runOnUiThread {
                        addMessage(ChatMessage(answer, isUser = false))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error de connexio: ${e.message}", Toast.LENGTH_LONG).show()
                    addMessage(ChatMessage("No he pogut connectar amb la IA.", isUser = false))
                }
            }
        }.start()
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }
}
