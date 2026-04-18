package com.example.testposturai.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditUserActivity : AppCompatActivity() {
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "EDITAR PERFIL"
            textSize = 20f
            setPadding(0, 0, 0, 40)
        }

        val inputNouEmail = EditText(this).apply {
            hint = "Nou correu"
            setText(authManager.getEmail())
        }

        val btnGuardar = Button(this).apply {
            text = "GUARDAR CANVIS"
            setOnClickListener {
                val emailAntic = authManager.getEmail()
                val email = inputNouEmail.text.toString().trim()
                android.util.Log.d("FireBase", "Intentant canviar a: '$email'")
                if (email.isNotEmpty()) {
                    authManager.actualitzarCorreu(email) { ok, error ->
                        if (ok) {
                            // 2. Si Auth diu OK, marquem a la base de dades que està pendent
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            val db = FirebaseFirestore.getInstance()

                            db.collection("users").document(userId!!).update("verificacioPendent", true).addOnSuccessListener {
                                    finish() // Tornem a la StartActivity
                            }

                        } else {
                            Toast.makeText(this@EditUserActivity, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        layout.addView(title)
        layout.addView(inputNouEmail)
        layout.addView(btnGuardar)
        setContentView(layout)
    }
}