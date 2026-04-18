package com.example.testposturai.auth

import com.google.firebase.auth.FirebaseAuth

class AuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun registrar(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun login(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun actualitzarCorreu(nouEmail: String, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser

        user?.verifyBeforeUpdateEmail(nouEmail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
    fun getEmail(): String? = auth.currentUser?.email
}