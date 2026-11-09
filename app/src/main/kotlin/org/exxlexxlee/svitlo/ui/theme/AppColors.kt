package org.exxlexxlee.svitlo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val Card: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val Title: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

    val Subtitle: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF8B95A7) else Color(0xFF9AA3B2)

    val Body: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFFA0AAB8) else Color(0xFF6B7488)

    val Accent = Color(0xFF5B6CFF)

    val AccentSoft: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF2B2F3A) else Color(0xFFE8EBFF)

    val Banner: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF252932) else Color(0xFFE4E6EC)

    val Track: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF2A2E38) else Color(0xFFD5D8E0)

    val Divider: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF2A2E38) else Color(0xFFEEF0F4)
}
