package com.example.gothere.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val canSubmit: Boolean = false
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun onEmailChange(v: String) {
        _ui.value = _ui.value.copy(
            email = v.trim(),
            error = null,
            canSubmit = isValid(v, _ui.value.password)
        )
    }

    fun onPasswordChange(v: String) {
        _ui.value = _ui.value.copy(
            password = v,
            error = null,
            canSubmit = isValid(_ui.value.email, v)
        )
    }

    fun togglePasswordVisibility() {
        _ui.value = _ui.value.copy(isPasswordVisible = !_ui.value.isPasswordVisible)
    }

    fun signIn(onSuccess: () -> Unit) {
        val email = _ui.value.email
        val password = _ui.value.password
        if (!isValid(email, password)) {
            _ui.value = _ui.value.copy(error = "Enter a valid email and password.")
            return
        }

        _ui.value = _ui.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val res = repo.signIn(email, password)
            _ui.value = if (res.isSuccess) {
                onSuccess()
                _ui.value.copy(isLoading = false, error = null)
            } else {
                _ui.value.copy(isLoading = false, error = res.exceptionOrNull()?.localizedMessage ?: "Sign in failed.")
            }
        }
    }

    private fun isValid(email: String, password: String): Boolean {
        val looksLikeEmail = email.contains("@") && email.contains(".")
        return looksLikeEmail && password.length >= 6
    }
}
