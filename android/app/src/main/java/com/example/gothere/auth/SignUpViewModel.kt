package com.example.gothere.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val canSubmit: Boolean = false
)

class SignUpViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(SignUpUiState())
    val ui: StateFlow<SignUpUiState> = _ui.asStateFlow()

    fun onEmailChange(v: String) = update(email = v.trim())
    fun onPasswordChange(v: String) = update(password = v)
    fun onConfirmChange(v: String) = update(confirm = v)
    fun togglePasswordVisibility() {
        _ui.value = _ui.value.copy(isPasswordVisible = !_ui.value.isPasswordVisible)
    }

    private fun update(
        email: String? = null,
        password: String? = null,
        confirm: String? = null
    ) {
        val next = _ui.value.copy(
            email = email ?: _ui.value.email,
            password = password ?: _ui.value.password,
            confirm = confirm ?: _ui.value.confirm,
            error = null
        )
        _ui.value = next.copy(canSubmit = isValid(next.email, next.password, next.confirm))
    }

    private fun isValid(email: String, password: String, confirm: String): Boolean {
        val emailOk = email.contains("@") && email.contains(".")
        val passOk = password.length >= 6
        return emailOk && passOk && password == confirm
    }

    fun createAccount(onSuccess: () -> Unit) {
        val s = _ui.value
        if (!isValid(s.email, s.password, s.confirm)) {
            _ui.value = s.copy(error = "Enter a valid email and matching passwords (6+ chars).")
            return
        }
        _ui.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val res = repo.createUser(s.email, s.password)
            _ui.value = if (res.isSuccess) {
                onSuccess()
                _ui.value.copy(isLoading = false, error = null)
            } else {
                _ui.value.copy(
                    isLoading = false,
                    error = res.exceptionOrNull()?.localizedMessage ?: "Sign up failed."
                )
            }
        }
    }
}
