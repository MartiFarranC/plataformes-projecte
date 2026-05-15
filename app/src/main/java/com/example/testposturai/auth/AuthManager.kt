package com.example.testposturai.auth

import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun registrar(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {

                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val dades = hashMapOf("verificacioPendent" to true)

                            db.collection("users").document(user.uid).set(dades)
                                .addOnSuccessListener {
                                    auth.signOut()
                                    callback(true, "T'hem enviat un correu de verificació. Revisa la bústia!")
                                }
                                .addOnFailureListener { e ->
                                    callback(false, "Error en crear perfil: ${e.message}")
                                }
                        } else {
                            callback(false, "Error en enviar correu: ${emailTask.exception?.message}")
                        }
                    }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun login(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(true, null)
                else callback(false, task.exception?.message)
            }
    }

    fun logout() {
        auth.signOut()
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

    fun isEmailVerified(): Boolean {
        val user = auth.currentUser
        return user?.isEmailVerified ?: false
    }

    fun actualitzarContrasenya(novaPass: String, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            user.updatePassword(novaPass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) callback(true, null)
                    else callback(false, task.exception?.message)
                }
        } else {
            callback(false, "No hi ha cap usuari autenticat")
        }
    }


    fun getEmail(): String? = auth.currentUser?.email

    fun eliminarCompte(callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            callback(false, "No hi ha cap usuari autenticat")
            return
        }

        val uid = user.uid
        db.collection("users").document(uid).delete()
            .addOnCompleteListener {
                user.delete().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        auth.signOut()
                        callback(true, null)
                    } else {
                        callback(false, deleteTask.exception?.message)
                    }
                }
            }
    }
}
