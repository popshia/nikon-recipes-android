package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Gold accent carried over from the iOS app's AccentColor.
private val GoldLight = Color(0xFF9C7D10)
private val GoldDark = Color(0xFFE5C158)

private val LightColors = lightColorScheme(
    primary = GoldLight,
    onPrimary = Color.White,
    secondary = GoldLight,
    tertiary = GoldLight,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFECECEF),
)

private val DarkColors = darkColorScheme(
    primary = GoldDark,
    onPrimary = Color(0xFF1C1B18),
    secondary = GoldDark,
    tertiary = GoldDark,
    background = Color(0xFF121415),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2A2A2C),
)

/** Material 3 Expressive theme, seeded with the iOS gold accent. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NikonTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
