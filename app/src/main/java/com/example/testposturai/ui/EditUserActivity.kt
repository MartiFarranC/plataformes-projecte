package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditUserActivity : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var btnGuardar: Button
    private lateinit var layout: LinearLayout
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        inputEmail = EditText(this).apply {
            hint = "Escriu el nou correu electrònic"
        }

        btnGuardar = Button(this).apply {
            text = "GUARDAR CANVIS"
            setOnClickListener {
                val nouEmail = inputEmail.text.toString().trim()
                guardarCanvis(nouEmail)
            }
        }

        layout.addView(inputEmail)
        layout.addView(btnGuardar)
        setContentView(layout)
    }

    private fun guardarCanvis(nouEmail: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null && nouEmail.isNotEmpty()) {
            db.collection("users").document(user.uid)
                .update("verificacioPendent", true)
                .addOnSuccessListener {
                    authManager.actualitzarCorreu(nouEmail) { ok, error ->
                        if (ok) {
                            Toast.makeText(this, "Correu de verificació enviat!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }
}