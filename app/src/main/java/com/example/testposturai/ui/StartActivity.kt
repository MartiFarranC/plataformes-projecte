package com.example.testposturai.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
    private lateinit var btnChatIa: Button
    private lateinit var btnEditarUsuari: Button
    private lateinit var btnLogout: Button
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

        btnChatIa = Button(this).apply {
            text = "XAT"
            textSize = 20f
            setOnClickListener {
                startActivity(Intent(this@StartActivity, ChatActivity::class.java))
            }
        }

        btnLogout = Button(this).apply {
            text = "TANCAR SESSIÓ"
            textSize = 20f
            setOnClickListener {
                authManager.logout()
                startActivity(Intent(this@StartActivity, AuthActivity::class.java))
                finish()
            }
        }
        setContentView(layout)
    }

    override fun onStart() {
        super.onStart()
        Log.d("StartActivity", "=== INICI ONSTART ===")

        val auth = FirebaseAuth.getInstance()
        var user = auth.currentUser

        if (user == null) {
            Log.d("StartActivity", "Status: Null user, redirigint a Auth")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        Log.d("StartActivity", "Usuari detectat: ${user.email}")

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val estaPendent = document.getBoolean("verificacioPendent") ?: false
            Log.d("StartActivity", "Firestore: estaPendent = $estaPendent")

            user!!.reload().addOnCompleteListener { task ->
                val userActualitzat = auth.currentUser
                val verificado = userActualitzat?.isEmailVerified ?: false

                Log.d("FireBase", "Dada real del servidor -> isEmailVerified: $verificado")

                if (estaPendent && verificado) {
                    Log.d("StartActivity", "LOGIC: Verificació completada amb èxit!")

                    db.collection("users").document(userActualitzat!!.uid).update("verificacioPendent", false)
                        .addOnSuccessListener {
                            Log.d("StartActivity", "DB: Variable 'verificacioPendent' posada a false")
                            Toast.makeText(this@StartActivity, "Correu verificat!", Toast.LENGTH_LONG).show()

                            auth.signOut()
                            startActivity(Intent(this@StartActivity, AuthActivity::class.java))
                            finish()
                        }
                } else if (estaPendent) {
                    Log.d("StartActivity", "LOGIC: Segueix pendent, mostrant avís vermell")
                    mostrarAvisVerificacio()
                } else {
                    Log.d("StartActivity", "LOGIC: Tot correcte, entrant a l'app")
                    mostrarInterficiePrincipal(userActualitzat?.email ?: "")
                }
            }
        }
    }

    private fun mostrarAvisVerificacio() {
        val user = FirebaseAuth.getInstance().currentUser
        layout.removeAllViews()

        val txtAvis = TextView(this).apply {
            text = "⚠️ VERIFICACIÓ PENDENT\n\nRevisa el teu correu i clica l'enllaç per continuar."
            gravity = android.view.Gravity.CENTER
            textSize = 18f
            setPadding(50, 50, 50, 20)
            setTextColor(Color.RED)
        }

        val btnComprovar = Button(this).apply {
            text = "JA HE CLICAT L'ENLLAÇ"
            setOnClickListener {
                onStart()
            }
        }

        val btnReenviar = Button(this).apply {
            text = "NO HE REBUT RES (REENVIAR)"
            setBackgroundColor(Color.LTGRAY)
            setOnClickListener {
                user?.sendEmailVerification()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@StartActivity, "Correu enviat de nou a ${user.email}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StartActivity, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        layout.addView(txtAvis)
        layout.addView(btnComprovar)
        layout.addView(btnReenviar)
    }

    private fun mostrarInterficiePrincipal(email: String) {
        layout.removeAllViews()
        txtNom.text = "$email, benvingut!"

        layout.addView(btnLogout)
        layout.addView(txtNom)
        layout.addView(btnStart)
        layout.addView(btnChatIa)
        layout.addView(btnEditarUsuari)
    }
}
