package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = BgLuxury,
    secondary = GoldAccent,
    onSecondary = BgLuxury,
    tertiary = MutedText,
    background = BgLuxury,
    onBackground = CreamText,
    surface = SurfaceLuxury,
    onSurface = CreamText,
    surfaceVariant = CardLuxury,
    onSurfaceVariant = CreamText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
