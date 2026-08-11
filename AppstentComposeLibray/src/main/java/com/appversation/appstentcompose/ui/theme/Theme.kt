package com.appversation.appstentcompose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    secondary = Teal200,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    secondary = Teal200,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val LegacyDarkColors = androidx.compose.material.darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200,
)

private val LegacyLightColors = androidx.compose.material.lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200,
)

@Composable
fun AppstentTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
    ) {
        // Authored text and legacy chrome retain their established pixel metrics while the
        // renderer's migrated components consume the outer Material 3 theme.
        androidx.compose.material.MaterialTheme(
            colors = if (darkTheme) LegacyDarkColors else LegacyLightColors,
            typography = LegacyTypography,
            shapes = LegacyShapes,
            content = content,
        )
    }
}
