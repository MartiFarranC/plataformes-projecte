package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
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
        setContentView(layout)
    }
}