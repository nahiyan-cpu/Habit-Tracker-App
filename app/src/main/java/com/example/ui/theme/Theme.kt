package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.HabitCatalog

data class HabitlySemanticColors(
    val success: Color = HabitlySuccess,
    val warning: Color = HabitlyWarning,
    val cardSurface: Color = HabitlyCardLight,
    val subtleSurface: Color = HabitlySurfaceVariantLight
)

val LocalHabitlyColors = staticCompositionLocalOf { HabitlySemanticColors() }

val HabitlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HabitlyTheme(
    themeMode: String = "system",
    accentColorHex: String = "#6C63FF",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val primaryAccent = HabitCatalog.parseColor(accentColorHex, HabitlyPrimary)
    val secondaryAccent = HabitlySecondary

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = secondaryAccent.copy(alpha = 0.18f),
            onSecondaryContainer = Color(0xFFE9D5FF),
            tertiary = HabitlySuccess,
            onTertiary = Color.White,
            background = HabitlyBgDark,
            onBackground = HabitlyTextPrimaryDark,
            surface = HabitlyCardDark,
            onSurface = HabitlyTextPrimaryDark,
            surfaceVariant = HabitlySurfaceVariantDark,
            onSurfaceVariant = HabitlyTextSecondaryDark,
            outline = HabitlyOutlineDark,
            error = HabitlyError,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.12f),
            onPrimaryContainer = primaryAccent,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = secondaryAccent.copy(alpha = 0.16f),
            onSecondaryContainer = Color(0xFF4C1D95),
            tertiary = HabitlySuccess,
            onTertiary = Color.White,
            background = HabitlyBgLight,
            onBackground = HabitlyTextPrimaryLight,
            surface = HabitlyCardLight,
            onSurface = HabitlyTextPrimaryLight,
            surfaceVariant = HabitlySurfaceVariantLight,
            onSurfaceVariant = HabitlyTextSecondaryLight,
            outline = HabitlyOutlineLight,
            error = HabitlyError,
            onError = Color.White
        )
    }

    val semanticColors = if (darkTheme) {
        HabitlySemanticColors(
            success = HabitlySuccess,
            warning = HabitlyWarning,
            cardSurface = HabitlyCardDark,
            subtleSurface = HabitlySurfaceVariantDark
        )
    } else {
        HabitlySemanticColors(
            success = HabitlySuccess,
            warning = HabitlyWarning,
            cardSurface = HabitlyCardLight,
            subtleSurface = HabitlySurfaceVariantLight
        )
    }

    CompositionLocalProvider(LocalHabitlyColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HabitlyTypography,
            shapes = HabitlyShapes,
            content = content
        )
    }
}
