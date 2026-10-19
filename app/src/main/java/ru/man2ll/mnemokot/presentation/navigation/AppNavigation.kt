package ru.man2ll.mnemokot.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
// Импортируем ваши реальные экраны из пакетов презентации проекта
import ru.man2ll.mnemokot.presentation.auth.AuthScreen
import ru.man2ll.mnemokot.presentation.generator.GeneratorScreen

/**
 * Безопасные константы маршрутов для Jetpack Compose Navigation.
 * Централизованное хранение предотвращает опечатки при переходах между экранами.
 */
object Routes {
    const val AUTH = "auth_route"
    const val GENERATOR = "generator_route"
}

/**
 * Архитектурный граф навигации MnemoKot.
 * Напрямую связывает точку входа приложения с вашими готовыми UI-компонентами.
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        // Оффлайн-менеджер паролей строго обязан стартовать с экрана проверки PIN-кода
        startDestination = Routes.AUTH 
    ) {
        // 1. Инициализация реального экрана авторизации
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    // При успешном вводе PIN-кода переводим пользователя на генератор
                    // Исключаем экран Auth из бэкстека, чтобы кнопка "Назад" не возвращала в заблокированное состояние
                    navController.navigate(Routes.GENERATOR) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // 2. Инициализация реального экрана мнемонического генератора (AMPG v2)
        composable(Routes.GENERATOR) {
            GeneratorScreen()
        }
    }
}
