package com.example.testposturai.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager

class StartActivity : AppCompatActivity() {
    private lateinit var txtNom: TextView
    val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val correu = authManager.getEmail() ?: "Usuari"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        txtNom = TextView(this).apply {
            textSize = 20f
            setPadding(30, 100, 30, 20)
            text = "${correu}, benvingut a PosturaCheck!"
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.LTGRAY)
        }

        val btnStart = Button(this).apply {
            text = "COMENÇAR ANÀLISI POSTURAL"
            textSize = 20f
            setOnClickListener {
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }

        layout.addView(btnStart)
        layout.addView(txtNom)
        setContentView(layout)
    }

    override fun onStart() {
        super.onStart() }
}