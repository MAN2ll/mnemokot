package ru.man2ll.mnemokot.presentation.navigation

/**
 * Герметичный класс для строго типизированного определения экранов приложения.
 * Помогает избежать опечаток в строковых маршрутах (routes).
 */
sealed class Screen(val route: String) {
    object Auth : Screen("auth_screen")
    object Generator : Screen("generator_screen")
    object Audit : Screen("audit_screen")
    object Settings : Screen("settings_screen")
}
