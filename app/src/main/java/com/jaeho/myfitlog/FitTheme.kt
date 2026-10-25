package com.jaeho.myfitlog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF087E78), onPrimary = Color.White,
    secondary = Color(0xFF087E78), secondaryContainer = Color(0xFFE4F3EF), onSecondaryContainer = Color(0xFF125B55),
    primaryContainer = Color(0xFFE4F3EF), onPrimaryContainer = Color(0xFF125B55),
    background = Color(0xFFF5F6F8), onBackground = Color(0xFF182529),
    surface = Color.White, onSurface = Color(0xFF182529),
    surfaceVariant = Color(0xFFEDF1F3), onSurfaceVariant = Color(0xFF53656B),
    surfaceTint = Color(0xFF087E78), surfaceDim = Color(0xFFD9E1E3), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F4F5),
    surfaceContainer = Color(0xFFEDF1F3), surfaceContainerHigh = Color(0xFFE7EEF0),
    surfaceContainerHighest = Color(0xFFE0E7E9),
    outlineVariant = Color(0xFFE0E7E9), outline = Color(0xFF708087),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF86D6C9), onPrimary = Color(0xFF003833),
    secondary = Color(0xFF86D6C9), secondaryContainer = Color(0xFF174D46), onSecondaryContainer = Color(0xFFB5EDE0),
    primaryContainer = Color(0xFF174D46), onPrimaryContainer = Color(0xFFB5EDE0),
    background = Color(0xFF101A1D), onBackground = Color(0xFFE0EBED),
    surface = Color(0xFF192629), onSurface = Color(0xFFE0EBED),
    surfaceVariant = Color(0xFF26363B), onSurfaceVariant = Color(0xFFB4C6CB),
    surfaceTint = Color(0xFF86D6C9), surfaceDim = Color(0xFF101A1D), surfaceBright = Color(0xFF304146),
    surfaceContainerLowest = Color(0xFF0C1518), surfaceContainerLow = Color(0xFF152226),
    surfaceContainer = Color(0xFF1A292D), surfaceContainerHigh = Color(0xFF203237),
    surfaceContainerHighest = Color(0xFF293D42),
    outlineVariant = Color(0xFF35494E), outline = Color(0xFF8EA3A9),
)

@Composable
fun FitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
