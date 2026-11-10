package org.exxlexxlee.svitlo.ui.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.ui.theme.LocalDarkTheme

internal class SchedulePalette(private val dark: Boolean) {
    val Screen: Color
        get() = if (dark) Color(0xFF121418) else Color(0xFFF2F4F8)

    val Card: Color
        get() = if (dark) Color(0xFF1E2128) else Color(0xFFFFFFFF)

    val Title: Color
        get() = if (dark) Color(0xFFF0F2F5) else Color(0xFF1B2437)

    val Muted = Color(0xFF8B95A7)

    val Off = Color(0xFFE23B3B)
    val On = Color(0xFFFFE826)
    val Yes = Color(0xFF2EAE4E)
    val Maybe = Color(0xFF4DAF98)

    val Unknown: Color
        get() = if (dark) Color(0xFF353C4A) else Color(0xFF2C3444)

    val Accent = Color(0xFF5B6CFF)
    val Marker = Color(0xFF35ED2F)

    val Banner: Color
        get() = if (dark) Color(0xFF252932) else Color(0xFFF0F1F4)

    val BannerText: Color
        get() = if (dark) Color(0xFFB0B7C3) else Color(0xFF6D7686)

    val GroupBadge: Color
        get() = if (dark) Color(0xFF252932) else Color(0xFFF0F1F4)

    val GroupBadgeText: Color
        get() = if (dark) Color(0xFFF0F2F5) else Color(0xFF1B2437)

    val Range = Color(0xFFE14D4D)
}

internal val LightSchedulePalette = SchedulePalette(false)
internal val DarkSchedulePalette = SchedulePalette(true)

internal val CardPalette: SchedulePalette
    @Composable get() = if (LocalDarkTheme.current) DarkSchedulePalette else LightSchedulePalette

internal fun SchedulePalette.stateColor(state: PowerState?): Color = when (state) {
    PowerState.ON -> On
    PowerState.OFF -> Off
    PowerState.MAYBE -> Maybe
    PowerState.UNKNOWN, null -> Unknown
}
