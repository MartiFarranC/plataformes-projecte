package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testposturai.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditUserActivity : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnEliminarCompte: Button
    private val authManager = AuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleScreen(layout)

        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleCard(card)

        val title = TextView(this).apply {
            text = "Editar perfil"
            gravity = Gravity.CENTER_HORIZONTAL
        }
        UiKit.styleTitle(title)

        val subtitle = TextView(this).apply {
            text = "Actualitza el teu correu electronic"
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, UiKit.dp(this@EditUserActivity, 8), 0, UiKit.dp(this@EditUserActivity, 14))
        }
        UiKit.styleSubtitle(subtitle)

        inputEmail = EditText(this).apply {
            hint = "Nou correu electronic"
        }
        UiKit.styleInput(inputEmail)

        btnGuardar = Button(this).apply {
            text = "Guardar canvis"
            setOnClickListener {
                val nouEmail = inputEmail.text.toString().trim()
                guardarCanvis(nouEmail)
            }
        }
        UiKit.stylePrimaryButton(btnGuardar)

        btnEliminarCompte = Button(this).apply {
            text = "Eliminar compte"
            setOnClickListener { confirmarEliminacioCompte() }
        }
        UiKit.styleDangerButton(btnEliminarCompte)

        val btnBack = Button(this).apply {
            text = "Tornar"
            setOnClickListener { finish() }
        }
        UiKit.styleGhostButton(btnBack)

        card.addView(title)
        card.addView(subtitle)
        card.addView(inputEmail)
        (inputEmail.layoutParams as LinearLayout.LayoutParams).bottomMargin = UiKit.dp(this, 14)
        card.addView(btnGuardar)
        (btnGuardar.layoutParams as LinearLayout.LayoutParams).bottomMargin = UiKit.dp(this, 10)
        card.addView(btnEliminarCompte)
        (btnEliminarCompte.layoutParams as LinearLayout.LayoutParams).bottomMargin = UiKit.dp(this, 10)
        card.addView(btnBack)

        layout.addView(card)
        (card.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 14)
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

    private fun confirmarEliminacioCompte() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar compte")
            .setMessage("Aquesta accio es permanent. Vols eliminar el teu compte de Firebase?")
            .setPositiveButton("Si, eliminar") { _, _ ->
                authManager.eliminarCompte { ok, error ->
                    runOnUiThread {
                        if (ok) {
                            Toast.makeText(this, "Compte eliminat correctament", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, AuthActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "No s'ha pogut eliminar: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
