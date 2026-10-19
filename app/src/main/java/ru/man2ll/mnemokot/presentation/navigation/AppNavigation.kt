package ru.man2ll.mnemokot.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Главный граф навигации приложения MnemoKot.
 * На данном этапе содержит базовые заглушки экранов для успешной компиляции.
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        // Стартовым ставим экран авторизации, так как это менеджер паролей
        startDestination = Screen.Auth.route 
    ) {
        // ЭКРАН 1: Авторизация (Вход по PIN-коду PBKDF2)
        composable(Screen.Auth.route) {
            DefaultStubScreen(title = "Экран авторизации (PIN)")
        }

        // ЭКРАН 2: Главный экран генератора (Мнемонический движок AMPG v2)
        composable(Screen.Generator.route) {
            DefaultStubScreen(title = "Мнемонический генератор паролей")
        }

        // ЭКРАН 3: Аудит безопасности паролей (passwordAuditor)
        composable(Screen.Audit.route) {
            DefaultStubScreen(title = "Аудит базы данных")
        }

        // ЭКРАН 4: Настройки и бэкапы (Backup v3 AES-GCM)
        composable(Screen.Settings.route) {
            DefaultStubScreen(title = "Настройки и Резервное копирование")
        }
    }
}

/**
 * Временный презентационный компонент-заглушка.
 * Будет заменен на реальные Compose-экраны из пакета presentation.
 */
@Composable
private fun DefaultStubScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}
