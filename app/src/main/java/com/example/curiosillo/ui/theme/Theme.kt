package com.example.curiosillo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary          = Primary,
    secondary        = Secondary,
    tertiary         = Tertiary,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVar,
    error            = Error,
    onPrimary        = OnPrimary,
    onSecondary      = OnSecondary,
    onBackground     = OnBackground,
    onSurface        = OnSurface,
    onSurfaceVariant = OnBackground,
)

private val DarkColors = darkColorScheme(
    primary          = PrimaryDark,
    secondary        = SecondaryDark,
    tertiary         = TertiaryDark,
    background       = BackgroundDark,
    surface          = SurfaceDark,
    surfaceVariant   = SurfaceVarDark,
    error            = Error,
    onPrimary        = OnPrimaryDark,
    onSecondary      = OnSecondaryDark,
    onBackground     = OnBackgroundDark,
    onSurface        = OnSurfaceDark,
    onSurfaceVariant = OnBackgroundDark,
)

@Composable
fun CuriosilloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
