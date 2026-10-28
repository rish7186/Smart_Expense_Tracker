package com.example.expense_tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.expense_tracker.R

private val Inter = FontFamily(Font(R.font.inter_variable, FontWeight.Normal))

private val LightColors = lightColorScheme(
    primary = FintechColors.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = FintechColors.lightPrimaryContainer,
    onPrimaryContainer = FintechColors.lightPrimary,
    secondary = Color(0xFF6B60C7),
    tertiary = FintechColors.lightIncome,
    background = FintechColors.lightBackground,
    onBackground = FintechColors.lightText,
    surface = FintechColors.lightSurface,
    onSurface = FintechColors.lightText,
    surfaceVariant = Color(0xFFEAE9F4),
    onSurfaceVariant = FintechColors.lightTextSecondary,
    error = FintechColors.lightExpense,
    errorContainer = Color(0xFFFFE5EA),
    onErrorContainer = Color(0xFF6B2437),
    outline = Color(0xFFD8D7E7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9A0FF),
    onPrimary = Color(0xFF17143A),
    primaryContainer = Color(0xFF34305D),
    onPrimaryContainer = FintechColors.textPrimary,
    secondary = FintechColors.accentTeal,
    onSecondary = FintechColors.background,
    secondaryContainer = Color(0xFF233A46),
    onSecondaryContainer = FintechColors.textPrimary,
    tertiary = FintechColors.income,
    background = FintechColors.background,
    onBackground = FintechColors.textPrimary,
    surface = FintechColors.surface,
    onSurface = FintechColors.textPrimary,
    surfaceVariant = Color(0xFF252941),
    onSurfaceVariant = FintechColors.textSecondary,
    error = FintechColors.expense,
    onError = FintechColors.background,
    errorContainer = Color(0xFF442638),
    onErrorContainer = FintechColors.textPrimary,
    outline = FintechColors.border,
)

private fun interStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Int = (size * 1.4).toInt(),
) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private val FintechTypography = Typography(
    displayLarge = interStyle(36, FontWeight.Bold, 44),
    displayMedium = interStyle(32, FontWeight.Bold, 40),
    displaySmall = interStyle(28, FontWeight.SemiBold, 36),
    headlineLarge = interStyle(28, FontWeight.Bold, 36),
    headlineMedium = interStyle(24, FontWeight.SemiBold, 32),
    headlineSmall = interStyle(20, FontWeight.SemiBold, 28),
    titleLarge = interStyle(20, FontWeight.SemiBold, 28),
    titleMedium = interStyle(16, FontWeight.SemiBold, 24),
    titleSmall = interStyle(14, FontWeight.SemiBold, 20),
    bodyLarge = interStyle(16, lineHeight = 24),
    bodyMedium = interStyle(14, lineHeight = 20),
    bodySmall = interStyle(12, lineHeight = 18),
    labelLarge = interStyle(14, FontWeight.SemiBold, 20),
    labelMedium = interStyle(12, FontWeight.Medium, 16),
    labelSmall = interStyle(11, FontWeight.Medium, 16),
)

@Composable
fun SmartExpenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = FintechTypography,
        content = content,
    )
}
