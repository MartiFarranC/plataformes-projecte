package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager

class AuthActivity : AppCompatActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "BENVINGUT"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }

        val inputEmail = EditText(this).apply { hint = "Email" }
        val inputPass = EditText(this).apply {
            hint = "Contrasenya"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val btnLogin = Button(this).apply {
            text = "INICIAR SESSIÓ"
            setOnClickListener {
                val email = inputEmail.text.toString().trim()
                val pass = inputPass.text.toString().trim()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    authManager.login(email, pass) { ok, error ->
                        if (ok) {
                            Toast.makeText(this@AuthActivity, "ok", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@AuthActivity, StartActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@AuthActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        val btnRegister = Button(this).apply {
            text = "CREAR COMPTE NOU"
            setOnClickListener {
                val email = inputEmail.text.toString().trim()
                val pass = inputPass.text.toString().trim()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    authManager.registrar(email, pass) { ok, error ->
                        if (ok) {
                            Toast.makeText(this@AuthActivity, "Compte creat! Iniciant...", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@AuthActivity, StartActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@AuthActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        layout.addView(title)
        layout.addView(inputEmail)
        layout.addView(inputPass)
        layout.addView(btnLogin)
        layout.addView(btnRegister)
        setContentView(layout)
    }
}