package com.example.gothere.ui.theme

import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gothere.R
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotMessage by remember { mutableStateOf<String?>(null) }

    val tealColor = Color(0xFF15B8A6)
    val privacyPolicyUrl = "https://gothere-app.web.app/gothere_privacy_policy.html"
    val checkInboxMsg = stringResource(R.string.auth_check_inbox)
    val validEmailMsg = stringResource(R.string.auth_valid_email)
    val resetFailedMsg = stringResource(R.string.auth_reset_failed)
    val enterBothMsg = stringResource(R.string.auth_enter_both)
    val pwShortMsg = stringResource(R.string.auth_pw_short)
    val authFailedMsg = stringResource(R.string.auth_failed)
    val agreePrefix = stringResource(R.string.auth_agree_prefix)
    val privacyLabel = stringResource(R.string.privacy_policy)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Forgot Password dialog
        if (showForgotPassword) {
            AlertDialog(
                onDismissRequest = { showForgotPassword = false; forgotMessage = null },
                title = { Text(stringResource(R.string.auth_reset_password)) },
                text = {
                    Column {
                        Text(stringResource(R.string.auth_reset_body))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text(stringResource(R.string.email)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (forgotMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = forgotMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (forgotMessage == checkInboxMsg) tealColor else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (forgotEmail.isBlank() || !forgotEmail.contains("@")) {
                            forgotMessage = validEmailMsg
                            return@TextButton
                        }
                        FirebaseAuth.getInstance().sendPasswordResetEmail(forgotEmail.trim())
                            .addOnCompleteListener { task ->
                                forgotMessage = if (task.isSuccessful) {
                                    checkInboxMsg
                                } else {
                                    task.exception?.localizedMessage ?: resetFailedMsg
                                }
                            }
                    }) { Text(stringResource(R.string.send)) }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPassword = false; forgotMessage = null }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Making the logo circular as requested
            Image(
                painter = painterResource(id = R.drawable.ic_logo_full),
                contentDescription = stringResource(R.string.logo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tealColor,
                    focusedLabelColor = tealColor,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tealColor,
                    focusedLabelColor = tealColor,
                    unfocusedBorderColor = tealColor 
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Main Action Button (Sign Up / Login)
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = enterBothMsg
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = pwShortMsg
                        return@Button
                    }
                    errorMessage = null
                    isLoading = true
                    try {
                        val auth = FirebaseAuth.getInstance()
                        val task = if (isLogin) {
                            auth.signInWithEmailAndPassword(email.trim(), password)
                        } else {
                            auth.createUserWithEmailAndPassword(email.trim(), password)
                        }

                        task.addOnCompleteListener { result ->
                            isLoading = false
                            if (result.isSuccessful) onAuthSuccess()
                            else errorMessage = result.exception?.localizedMessage ?: authFailedMsg
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.localizedMessage ?: authFailedMsg
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tealColor),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(if (isLogin) R.string.log_in else R.string.sign_up), fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle between Login and Sign Up
            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = stringResource(if (isLogin) R.string.auth_no_account else R.string.auth_have_account),
                    color = tealColor,
                    fontSize = 14.sp
                )
            }

            // Forgot Password (only show on login)
            if (isLogin) {
                TextButton(onClick = {
                    forgotEmail = email
                    showForgotPassword = true
                }) {
                    Text(
                        text = stringResource(R.string.auth_forgot),
                        color = tealColor.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Policy link
            val annotatedText = buildAnnotatedString {
                append(agreePrefix)
                pushStringAnnotation(tag = "URL", annotation = privacyPolicyUrl)
                withStyle(SpanStyle(color = tealColor, textDecoration = TextDecoration.Underline)) {
                    append(privacyLabel)
                }
                pop()
            }
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                        }
                }
            )
        }

        // Theme Toggle Icon at the bottom right
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = stringResource(R.string.toggle_theme),
                tint = if (isDark) Color.White else Color.Black
            )
        }
    }
}
