package org.exxlexxlee.svitlo.ui.schedule

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider as dayNightColorProvider
import androidx.glance.unit.ColorProvider

/** Explicit public factory; keep the widget's chosen palette in either system theme. */
internal fun glanceColor(color: Color): ColorProvider =
    dayNightColorProvider(day = color, night = color)
