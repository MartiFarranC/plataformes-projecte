package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager

class StartActivity : AppCompatActivity() {

    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        val inputEmail = EditText(this).apply {
            hint = "Email"
        }

        val inputPass = EditText(this).apply {
            hint = "Contrasenya"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val btnLogin = Button(this).apply {
            text = "ENTRAR"
            setOnClickListener {
                val email = inputEmail.text.toString()
                val pass = inputPass.text.toString()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    authManager.login(email, pass) { ok, error ->
                        if (ok) saltarAMain()
                        else Toast.makeText(this@StartActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val btnRegister = Button(this).apply {
            text = "CREAR COMPTE"
            setOnClickListener {
                val email = inputEmail.text.toString()
                val pass = inputPass.text.toString()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    authManager.registrar(email, pass) { ok, error ->
                        if (ok) Toast.makeText(this@StartActivity, "Compte creat! Fes login.", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this@StartActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }



        val btnStart = Button(this).apply {
            text = "COMENÇAR ANÀLISI POSTURAL"
            textSize = 20f
            setOnClickListener {
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }

        layout.addView(inputEmail)
        layout.addView(inputPass)
        layout.addView(btnLogin)
        layout.addView(btnRegister)
        layout.addView(btnStart)
        setContentView(layout)
    }
    private fun saltarAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onStart() {
        super.onStart() }
}