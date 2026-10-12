package ru.man2ll.mnemokot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),       // Фирменный зеленый "Кот"
    secondary = Color(0xFF8BC34A),
    background = Color(0xFF121212),    // Глубокий темный фон для безопасности
    surface = Color(0xFF1E1E1E)
)

@Composable
fun MnemoKotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
