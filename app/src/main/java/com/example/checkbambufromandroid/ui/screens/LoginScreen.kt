package com.example.checkbambufromandroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.checkbambufromandroid.R
import com.example.checkbambufromandroid.ui.theme.CheckBambuFromAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    needsVerification: Boolean,
    onLoginClicked: (String, String) -> Unit,
    onVerifyClicked: (String, String, String) -> Unit,
    onLoginSuccess: () -> Unit, // Kept for preview, but logic is now in AppNavigation
    clearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    // Clear error when screen composition changes in a way that should reset it,
    // e.g. when needsVerification changes.
    LaunchedEffect(needsVerification) {
        clearError()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(id = R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.login_email_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = error != null && !needsVerification // Show error on email/pass if not in verification step
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.login_password_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = error != null && !needsVerification // Show error on email/pass if not in verification step
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (needsVerification) {
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { verificationCode = it },
                    label = { Text(stringResource(id = R.string.login_verification_code_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null && needsVerification // Show error on code if in verification step
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        clearError()
                        onVerifyClicked(email, password, verificationCode)
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && verificationCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(stringResource(id = R.string.login_button_verify))
                }
            } else {
                Button(
                    onClick = {
                        clearError()
                        onLoginClicked(email, password)
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(stringResource(id = R.string.login_button_login))
                }
            }

            error?.let {
                // Only show error if it's relevant to the current step, or a general error
                val showError = (needsVerification && (it.contains("code", ignoreCase = true) || it.contains("verification", ignoreCase = true))) ||
                                (!needsVerification && (it.contains("credentials", ignoreCase = true) || it.contains("Login failed", ignoreCase = true) || it.contains("password", ignoreCase = true))) ||
                                // Show general errors not specific to a field
                                (!it.contains("code", ignoreCase = true) && !it.contains("verification", ignoreCase = true) && !it.contains("credentials", ignoreCase = true) && !it.contains("Login failed", ignoreCase = true) && !it.contains("password", ignoreCase = true))


                if (showError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    CheckBambuFromAndroidTheme {
        LoginScreen(false, null, false, { _, _ -> }, { _, _, _ -> }, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenNeedsVerificationPreview() {
    CheckBambuFromAndroidTheme {
        LoginScreen(false, "Verification code sent.", true, { _, _ -> }, { _, _, _ -> }, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLoadingPreview() {
    CheckBambuFromAndroidTheme {
        LoginScreen(true, null, false, { _, _ -> }, { _, _, _ -> }, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenErrorPreview() {
    CheckBambuFromAndroidTheme {
        LoginScreen(false, "Invalid credentials.", false, { _, _ -> }, { _, _, _ -> }, {}, {})
    }
}
