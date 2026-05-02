package com.example.testposturai.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
            gravity = Gravity.CENTER
        }
        UiKit.styleScreen(layout)

        txtNom = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@StartActivity, 8), 0, UiKit.dp(this@StartActivity, 20))
            setTextColor(UiKit.colorSecondaryText)
        }

        btnStart = Button(this).apply {
            text = "Comencar analisi postural"
            setOnClickListener { startActivity(Intent(this@StartActivity, MainActivity::class.java)) }
        }
        UiKit.stylePrimaryButton(btnStart)

        btnEditarUsuari = Button(this).apply {
            text = "Editar el meu perfil"
            setOnClickListener { startActivity(Intent(this@StartActivity, EditUserActivity::class.java)) }
        }
        UiKit.styleSecondaryButton(btnEditarUsuari)

        btnChatIa = Button(this).apply {
            text = "Xat IA"
            setOnClickListener { startActivity(Intent(this@StartActivity, ChatActivity::class.java)) }
        }
        UiKit.styleSecondaryButton(btnChatIa)

        btnLogout = Button(this).apply {
            text = "Tancar sessio"
            setOnClickListener {
                authManager.logout()
                startActivity(Intent(this@StartActivity, AuthActivity::class.java))
                finish()
            }
        }
        UiKit.styleSecondaryButton(btnLogout)

        setContentView(layout)
    }

    override fun onStart() {
        super.onStart()
        Log.d("StartActivity", "=== INICI ONSTART ===")

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val estaPendent = document.getBoolean("verificacioPendent") ?: false

            user.reload().addOnCompleteListener {
                val userActualitzat = auth.currentUser
                val verificat = userActualitzat?.isEmailVerified ?: false

                if (estaPendent && verificat) {
                    db.collection("users").document(user.uid).update("verificacioPendent", false)
                        .addOnSuccessListener {
                            Toast.makeText(this@StartActivity, "Correu verificat!", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            startActivity(Intent(this@StartActivity, AuthActivity::class.java))
                            finish()
                        }
                } else if (estaPendent) {
                    mostrarAvisVerificacio()
                } else {
                    mostrarInterficiePrincipal(userActualitzat?.email ?: "")
                }
            }
        }
    }

    private fun mostrarAvisVerificacio() {
        val user = FirebaseAuth.getInstance().currentUser
        layout.removeAllViews()

        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleCard(card)

        val txtAvis = TextView(this).apply {
            text = "Verificacio pendent\n\nRevisa el correu i clica l'enllac per continuar."
            gravity = Gravity.CENTER
        }
        UiKit.styleTitle(txtAvis)
        txtAvis.textSize = 20f
        txtAvis.setTextColor(UiKit.colorDanger)

        val btnComprovar = Button(this).apply {
            text = "Ja he clicat l'enllac"
            setOnClickListener { onStart() }
        }
        UiKit.stylePrimaryButton(btnComprovar)

        val btnReenviar = Button(this).apply {
            text = "Reenviar correu"
            setOnClickListener {
                user?.sendEmailVerification()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@StartActivity, "Correu reenviat a ${user.email}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StartActivity, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        UiKit.styleSecondaryButton(btnReenviar)

        card.addView(txtAvis)
        card.addView(btnComprovar)
        (btnComprovar.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 18)
        card.addView(btnReenviar)
        (btnReenviar.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 10)

        layout.addView(card)
    }

    private fun mostrarInterficiePrincipal(email: String) {
        layout.removeAllViews()

        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        UiKit.styleCard(card)

        val title = TextView(this).apply {
            text = "Panell principal"
            gravity = Gravity.CENTER
        }
        UiKit.styleTitle(title)

        txtNom.text = "$email, benvingut"
        txtNom.gravity = Gravity.CENTER

        card.addView(title)
        addSafely(card, txtNom)
        addSafely(card, btnStart)
        (btnStart.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 8)
        addSafely(card, btnChatIa)
        (btnChatIa.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 10)
        addSafely(card, btnEditarUsuari)
        (btnEditarUsuari.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 10)
        addSafely(card, btnLogout)
        (btnLogout.layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this, 18)

        layout.addView(card)
    }

    private fun addSafely(parent: LinearLayout, child: View) {
        (child.parent as? ViewGroup)?.removeView(child)
        parent.addView(child)
    }
}
