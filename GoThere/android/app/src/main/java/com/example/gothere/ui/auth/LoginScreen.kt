package com.example.gothere.ui.auth   // <- change to your package if different

@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gothere.R
import com.example.gothere.ui.theme.GoThereTheme

@Composable
fun LoginScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    // UI state
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // brand teal for the whole screen background
    val Teal = Color(0xFF17C9C0)

    GoThereTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Teal                                // <<< teal background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Logo
                        Image(
                            painter = painterResource(R.drawable.gothere_logo),
                            contentDescription = "GoThere",
                            modifier = Modifier
                                .width(140.dp)
                                .height(48.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "GoThere — Sign in",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(20.dp))

                        // EMAIL
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", color = Color.Black) },   // <<< black label
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color.Black), // <<< black text
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
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
                            label = { Text("Password", color = Color.Black) }, // <<< black label
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = LocalTextStyle.current.copy(color = Color.Black), // <<< black text
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Black.copy(alpha = 0.4f),
                                cursorColor = Color.Black
                            )
                        )

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { onLogin(email, password) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sign in")
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(onClick = onNavigateToSignUp) {
                            Text("Create account")
                        }

                        Spacer(Modifier.height(8.dp))

                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                }
            }
        }
    }
}
