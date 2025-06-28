package com.example.checkbambufromandroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.checkbambufromandroid.ui.screens.LoginScreen
import com.example.checkbambufromandroid.ui.screens.PrinterStatusScreen
import com.example.checkbambufromandroid.ui.viewmodels.LoginViewModel
import com.example.checkbambufromandroid.ui.viewmodels.PrinterStatusViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.LoginScreen.route) {
        composable(Screen.LoginScreen.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val loginState by loginViewModel.loginState.collectAsState()

            LaunchedEffect(loginState.loginSuccess) {
                if (loginState.loginSuccess) {
                    navController.navigate(Screen.PrinterStatusScreen.route) {
                        popUpTo(Screen.LoginScreen.route) { inclusive = true }
                    }
                    loginViewModel.resetNavigation() // Reset flag after navigation
                }
            }
            // Clear error on screen entry or when certain conditions change if needed
            // LaunchedEffect(Unit) { loginViewModel.clearError() }


            LoginScreen(
                isLoading = loginState.isLoading,
                error = loginState.error,
                needsVerification = loginState.needsVerification,
                onLoginClicked = { email, password -> loginViewModel.login(email, password) },
                onVerifyClicked = { _, _, code -> loginViewModel.verifyCode(code) }, // email & pw are now stored in VM
                onLoginSuccess = { /* Handled by LaunchedEffect */ },
                clearError = { loginViewModel.clearError() }
            )
        }
        composable(Screen.PrinterStatusScreen.route) {
            val printerStatusViewModel: PrinterStatusViewModel = hiltViewModel()
            val printerStatusState by printerStatusViewModel.statusState.collectAsState()

            LaunchedEffect(printerStatusState.navigateToLogin) {
                if (printerStatusState.navigateToLogin) {
                    navController.navigate(Screen.LoginScreen.route) {
                        popUpTo(Screen.PrinterStatusScreen.route) { inclusive = true }
                    }
                    printerStatusViewModel.resetNavigation() // Reset flag
                }
            }
             // LaunchedEffect(Unit) { printerStatusViewModel.clearError() }

            PrinterStatusScreen(
                isLoading = printerStatusState.isLoading,
                error = printerStatusState.error,
                printerStatus = printerStatusState.printerStatus,
                onRefreshClicked = { printerStatusViewModel.fetchPrinterStatus() },
                onLogout = { printerStatusViewModel.userLogout() },
                clearError = { printerStatusViewModel.clearError() }
            )
        }
    }
}
