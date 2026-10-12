package com.example.expense_tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF256D5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F2E6),
    secondary = Color(0xFF49645B),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF1),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91D5BE),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF155143),
    secondary = Color(0xFFB2CCC1),
    background = Color(0xFF101513),
    surface = Color(0xFF181F1C),
    surfaceVariant = Color(0xFF29332F),
)

@Composable
fun SmartExpenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
