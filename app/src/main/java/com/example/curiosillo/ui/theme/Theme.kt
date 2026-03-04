package com.example.curiosillo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Colors = lightColorScheme(
    primary      = Primary,
    secondary    = Secondary,
    tertiary     = Tertiary,
    background   = Background,
    surface      = Surface,
    error        = Error,
    onPrimary    = OnPrimary,
    onSecondary  = OnSecondary,
    onBackground = OnBackground,
    onSurface    = OnBackground,
)

@Composable
fun CuriosilloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
