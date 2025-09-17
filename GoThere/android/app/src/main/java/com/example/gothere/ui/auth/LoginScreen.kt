package com.example.gothere.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.R
import com.example.gothere.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit
) {
    val authVM: AuthViewModel = viewModel()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var revealPw by remember { mutableStateOf(false) }

    val focus = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Logo ───────────────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.ic_gothere_logo),
                contentDescription = "GoThere logo",
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))
            Text("GoThere — Sign in", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (revealPw) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val cd = if (revealPw) "Hide password" else "Show password"
                    Icon(
                        imageVector = if (revealPw) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = cd,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { revealPw = !revealPw }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focus.clearFocus()
                        if (!isLoading) doSignIn(
                            email, password, authVM,
                            setBusy = { isLoading = it },
                            setError = { errorMsg = it },
                            onLoggedIn = onLoggedIn
                        )
                    }
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    focus.clearFocus()
                    doSignIn(
                        email, password, authVM,
                        setBusy = { isLoading = it },
                        setError = { errorMsg = it },
                        onLoggedIn = onLoggedIn
                    )
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .semantics { contentDescription = "Signing in" }
                    )
                } else {
                    Text("Sign in")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Create account",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isLoading) {
                        focus.clearFocus()
                        doSignUp(
                            email, password, authVM,
                            setBusy = { isLoading = it },
                            setError = { errorMsg = it },
                            onLoggedIn = onLoggedIn
                        )
                    }
                    .padding(8.dp)
            )

            errorMsg?.let {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = { errorMsg = null },
                    label = { Text(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun doSignIn(
    email: String,
    password: String,
    vm: AuthViewModel,
    setBusy: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onLoggedIn: () -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        setError("Enter email and password")
        return
    }
    setBusy(true)
    vm.signIn(email, password) { result ->
        setBusy(false)
        result.onSuccess {
            setError(null)
            onLoggedIn()
        }.onFailure { e ->
            setError(e.message ?: "Sign in failed")
        }
    }
}

private fun doSignUp(
    email: String,
    password: String,
    vm: AuthViewModel,
    setBusy: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onLoggedIn: () -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        setError("Enter email and password")
        return
    }
    setBusy(true)
    vm.signUp(email, password) { result ->
        setBusy(false)
        result.onSuccess {
            setError(null)
            onLoggedIn()
        }.onFailure { e ->
            setError(e.message ?: "Sign up failed")
        }
    }
}
