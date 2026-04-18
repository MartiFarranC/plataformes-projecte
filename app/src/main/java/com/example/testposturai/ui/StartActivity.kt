package com.example.testposturai.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class StartActivity : AppCompatActivity() {
    private lateinit var layout: LinearLayout
    private lateinit var txtNom: TextView
    private lateinit var btnStart: Button
    private lateinit var btnEditarUsuari: Button
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        txtNom = TextView(this).apply {
            textSize = 20f
            setPadding(30, 100, 30, 20)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.LTGRAY)
        }

        btnStart = Button(this).apply {
            text = "COMENÇAR ANÀLISI POSTURAL"
            textSize = 20f
            setOnClickListener {
                startActivity(Intent(this@StartActivity, MainActivity::class.java))
            }
        }

        btnEditarUsuari = Button(this).apply {
            text = "EDITAR EL MEU PERFIL"
            textSize = 20f
            setOnClickListener {
                startActivity(Intent(this@StartActivity, EditUserActivity::class.java))
            }
        }
        setContentView(layout)
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user == null) { // Si no user --> login
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val estaPendent = if (document.exists()) {
                document.getBoolean("verificacioPendent") ?: false
            } else {
                false
            }


            user.reload().addOnCompleteListener {
                if (estaPendent && user.isEmailVerified) {
                    db.collection("users").document(user.uid).update("verificacioPendent", false)
                    mostrarInterficiePrincipal(user.email ?: "")
                } else if (estaPendent) {
                    mostrarAvisVerificacio()
                } else {
                    mostrarInterficiePrincipal(user.email ?: "")
                }
            }
        }
    }

    private fun mostrarAvisVerificacio() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val emailPendent = prefs.getString("email_pendent", "el teu nou correu")

        layout.removeAllViews()
        val txtAvis = TextView(this).apply {
            text = "⚠️ CONFIRMACIÓ PENDENT\n\nHem enviat un enllaç a:\n$emailPendent\n\nRevisa l'Spam!"
            gravity = android.view.Gravity.CENTER
            textSize = 18f
            setPadding(50, 50, 50, 50)
            setTextColor(Color.RED)
        }

        val btnComprovar = Button(this).apply {
            text = "JA HE CLICAT L'ENLLAÇ"
            setOnClickListener {
                onStart()
                Toast.makeText(this@StartActivity, "Comprovant estat...", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(txtAvis)
        layout.addView(btnComprovar)
    }

    private fun mostrarInterficiePrincipal(email: String) {
        layout.removeAllViews()
        txtNom.text = "$email, benvingut!"

        layout.addView(txtNom)
        layout.addView(btnStart)
        layout.addView(btnEditarUsuari)
    }
}