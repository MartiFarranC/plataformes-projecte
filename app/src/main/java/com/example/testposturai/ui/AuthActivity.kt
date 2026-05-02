package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        UiKit.styleScreen(layout)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        UiKit.styleCard(card)

        val title = TextView(this).apply {
            text = "BENVINGUT"
            gravity = Gravity.CENTER
        }
        UiKit.styleTitle(title)

        val subtitle = TextView(this).apply {
            text = "Inicia sessio per continuar"
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@AuthActivity, 8), 0, UiKit.dp(this@AuthActivity, 20))
        }
        UiKit.styleSubtitle(subtitle)

        val inputEmail = EditText(this).apply { hint = "Email" }
        UiKit.styleInput(inputEmail)

        val inputPass = EditText(this).apply {
            hint = "Contrasenya"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        UiKit.styleInput(inputPass)

        val btnLogin = Button(this).apply {
            text = "Iniciar sessio"
            setOnClickListener {
                val email = inputEmail.text.toString().trim()
                val pass = inputPass.text.toString().trim()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    authManager.login(email, pass) { ok, error ->
                        if (ok) {
                            Toast.makeText(this@AuthActivity, "ok", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@AuthActivity, StartActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@AuthActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        UiKit.stylePrimaryButton(btnLogin)

        val btnRegister = Button(this).apply {
            text = "Crear compte nou"
            setOnClickListener {
                val email = inputEmail.text.toString().trim()
                val pass = inputPass.text.toString().trim()
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    authManager.registrar(email, pass) { ok, missatge ->
                        if (ok) {
                            Toast.makeText(this@AuthActivity, missatge, Toast.LENGTH_LONG).show()
                            inputEmail.text.clear()
                            inputPass.text.clear()
                        } else {
                            Toast.makeText(this@AuthActivity, "Error: $missatge", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        UiKit.styleSecondaryButton(btnRegister)

        val space = UiKit.dp(this, 12)
        card.addView(title)
        card.addView(subtitle)
        card.addView(inputEmail)
        (inputEmail.layoutParams as LinearLayout.LayoutParams).bottomMargin = space
        card.addView(inputPass)
        (inputPass.layoutParams as LinearLayout.LayoutParams).bottomMargin = UiKit.dp(this, 16)
        card.addView(btnLogin)
        (btnLogin.layoutParams as LinearLayout.LayoutParams).bottomMargin = space
        card.addView(btnRegister)

        layout.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        val user = FirebaseAuth.getInstance().currentUser

        user?.reload()?.addOnCompleteListener {
            if (user.isEmailVerified) {
                Toast.makeText(this, "Correu verificat!", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this, "Correu no verificat. Revisa la teva bústia!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}