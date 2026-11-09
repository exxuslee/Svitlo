package org.exxlexxlee.svitlo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Amber80,
    secondary = AmberGrey80,
    tertiary = Teal80,
    primaryContainer = Color(0xFF5D4A00),
    onPrimaryContainer = Color(0xFFFFE082),
    background = Color(0xFF121418),
    surface = Color(0xFF1E2128),
    onBackground = Color(0xFFF0F2F5),
    onSurface = Color(0xFFF0F2F5),
)

private val LightColorScheme = lightColorScheme(
    primary = Amber40,
    secondary = AmberGrey40,
    tertiary = Teal40,
    primaryContainer = Color(0xFFFFF3C4),
    onPrimaryContainer = Color(0xFF3E2F00),
    background = Color(0xFFF2F4F8),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B2437),
    onSurface = Color(0xFF1B2437),
)

@Composable
fun SvitloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
