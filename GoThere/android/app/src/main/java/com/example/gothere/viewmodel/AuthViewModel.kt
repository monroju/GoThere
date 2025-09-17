package com.example.gothere.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val listener = FirebaseAuth.AuthStateListener { fb ->
        _user.value = fb.currentUser
    }

    init {
        auth.addAuthStateListener(listener)
    }

    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { t ->
                    if (t.isSuccessful) onResult(Result.success(Unit))
                    else onResult(Result.failure(t.exception ?: Exception("Sign in failed")))
                }
        }
    }

    fun signUp(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { t ->
                    if (t.isSuccessful) onResult(Result.success(Unit))
                    else onResult(Result.failure(t.exception ?: Exception("Sign up failed")))
                }
        }
    }

    fun logout() {
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(listener)
    }
}
