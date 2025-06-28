package com.example.checkbambufromandroid.ui.navigation

sealed class Screen(val route: String) {
    object LoginScreen : Screen("login_screen")
    object PrinterStatusScreen : Screen("printer_status_screen")
}
