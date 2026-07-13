package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Full Material 3 tonal scheme seeded from the iOS app's gold accent (#9C7D10),
// so every role — primary, containers, surfaces, outline — is gold-harmonized
// instead of falling back to M3's default purple baseline.
private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA1),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Color(0xFF6D5C3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7E0BB),
    onSecondaryContainer = Color(0xFF251A04),
    tertiary = Color(0xFF51643F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3EABB),
    onTertiaryContainer = Color(0xFF0F2004),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF1F1B13),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF1F1B13),
    surfaceVariant = Color(0xFFEDE1CF),
    onSurfaceVariant = Color(0xFF4D4639),
    surfaceContainer = Color(0xFFF4EADD),
    surfaceContainerHigh = Color(0xFFEEE4D7),
    outline = Color(0xFF7F7767),
    outlineVariant = Color(0xFFD0C5B3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF5BF48),
    onPrimary = Color(0xFF402D00),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFDEA1),
    secondary = Color(0xFFDAC4A0),
    onSecondary = Color(0xFF3A2F15),
    secondaryContainer = Color(0xFF52452A),
    onSecondaryContainer = Color(0xFFF7E0BB),
    tertiary = Color(0xFFB8CEA1),
    onTertiary = Color(0xFF243515),
    tertiaryContainer = Color(0xFF3A4C2A),
    onTertiaryContainer = Color(0xFFD3EABB),
    background = Color(0xFF16130B),
    onBackground = Color(0xFFEAE1D2),
    surface = Color(0xFF16130B),
    onSurface = Color(0xFFEAE1D2),
    surfaceVariant = Color(0xFF4D4639),
    onSurfaceVariant = Color(0xFFD0C5B3),
    surfaceContainer = Color(0xFF231F16),
    surfaceContainerHigh = Color(0xFF2E2920),
    outline = Color(0xFF999080),
    outlineVariant = Color(0xFF4D4639),
)

/** Material 3 theme, fully seeded with the iOS gold accent. */
@Composable
fun NikonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
