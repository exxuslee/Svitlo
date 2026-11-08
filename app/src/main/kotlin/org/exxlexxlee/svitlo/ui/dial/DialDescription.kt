package org.exxlexxlee.svitlo.ui.dial

import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.PowerState

fun buildDialDescription(hours: List<HourState>, hourOffset: Int): String {
    val halves = hours.flatMap { listOf(it.first, it.second) }
    if (halves.all { it == PowerState.ON }) {
        return "світло є протягом усієї половини доби"
    }
    if (halves.all { it == PowerState.OFF }) {
        return "світла немає протягом усієї половини доби"
    }

    val parts = mutableListOf<String>()
    var index = 0
    while (index < halves.size) {
        val state = halves[index]
        if (state == PowerState.ON || state == PowerState.UNKNOWN) {
            index += 1
            continue
        }
        var end = index + 1
        while (end < halves.size && halves[end] == state) {
            end += 1
        }
        val startHour = hourOffset + index / 2
        val startMinute = if (index % 2 == 0) "00" else "30"
        val endTotalMinutes = (hourOffset * 60) + end * 30
        val endHour = (endTotalMinutes / 60).let { if (it == 24) 24 else it }
        val endMinute = (endTotalMinutes % 60).toString().padStart(2, '0')
        val range = "з $startHour:$startMinute до $endHour:$endMinute"
        parts += if (state == PowerState.MAYBE) {
            "$range можливе відключення"
        } else {
            "$range світла немає"
        }
        index = end
    }
    return parts.joinToString(", ").ifBlank { "немає даних про відключення" }
}
