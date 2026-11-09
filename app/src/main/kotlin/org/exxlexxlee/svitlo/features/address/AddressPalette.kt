package org.exxlexxlee.svitlo.features.address

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.exxlexxlee.svitlo.ui.theme.LocalDarkTheme

object AddressPalette {
    val Background: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF121418) else Color(0xFFF2F4F8)

    val Card: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFF1E2128) else Color(0xFFFFFFFF)

    val Title: Color
        @Composable get() = if (LocalDarkTheme.current) Color(0xFFF0F2F5) else Color(0xFF1B2437)

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
