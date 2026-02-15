@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.R

/**
 * Login screen styled to match the mock:
 * - Teal background
 * - Centered card with rounded corners
 * - Logo centered in card
 * - “Welcome Back!”
 * - Email + Password fields
 * - Teal “Log In” button
 * - “Don’t have an account? Sign Up” link
 * - Dark/Light toggle top-right
 */
@Composable
fun LoginScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    // Local UI state
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Colors to match the screenshot
    val Teal = Color(0xFF17C9C0)   // button / accents
    val TealBg = Color(0xFFE6F7F6) // page background
    val CardCorner = 16.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TealBg
    ) {
        // Top-right theme toggle (floated)
        Box(Modifier.fillMaxSize()) {
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }

            // Main auth card
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(CardCorner),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "GoThere",
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Welcome Back!",
                        color = Color(0xFF33B0A9),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // EMAIL
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = Color.Black) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.4f),
                            cursorColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // PASSWORD
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.Black) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.4f),
                            cursorColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Log In
                    Button(
                        onClick = { onLogin(email.trim(), password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        enabled = email.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Log In")
                    }

                    Spacer(Modifier.height(12.dp))

                    // Sign up link
                    TextButton(onClick = onNavigateToSignUp) {
                        val text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append("Don't have an account? ")
                            }
                            withStyle(SpanStyle(color = Teal)) {
                                append("Sign Up")
                            }
                        }
                        Text(text)
                    }
                }
            }
        }
    }
}
