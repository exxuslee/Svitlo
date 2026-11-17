package org.exxlexxlee.svitlo.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import org.exxlexxlee.domain.model.*
import org.exxlexxlee.svitlo.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun OutageSnapshotContent(state: OutageCardState, now: Instant, cardId: Long) {
    when (state) {
        OutageCardState.Loading -> CircularProgressIndicator(Modifier.size(24.dp))
        OutageCardState.Error -> Text(stringResource(R.string.outage_load_error), color = CardPalette.Off)
        is OutageCardState.Content -> SnapshotContent(state.snapshot, now, cardId)
        is OutageCardState.Stale -> {
            Text(stringResource(R.string.outage_stale, maxOf(0, Duration.between(state.snapshot.fetchedAt, now).toMinutes())), color = CardPalette.Muted)
            SnapshotContent(state.snapshot, now, cardId)
        }
    }
}

@Composable
private fun SnapshotContent(snapshot: OutageSnapshot, now: Instant, cardId: Long) {
    val locale = LocalConfiguration.current.locales[0]
    when (snapshot) {
        is OutageSnapshot.Timeline -> TimelineContent(snapshot, now, cardId)
        is OutageSnapshot.AreaSummary -> {
            Text(snapshot.areaName, color = CardPalette.Title)
            val count = snapshot.customersWithoutPower
            Text(when (count) {
                null -> stringResource(R.string.outage_area_no_data)
                0 -> stringResource(R.string.outage_area_normal)
                else -> stringResource(R.string.outage_customers, count)
            }, color = CardPalette.Title)
            snapshot.percentWithoutPower?.let {
                Text(stringResource(R.string.outage_percent, String.format(locale, "%.1f", it)), color = CardPalette.Muted)
            }
        }
    }
    if (snapshot.locationMatch != LocationMatch.EXACT_ADDRESS) {
        Text(stringResource(R.string.outage_area_only), color = CardPalette.Muted)
    }
    Text(
        stringResource(R.string.outage_source, snapshot.sourceName,
            DateTimeFormatter.ofPattern("dd.MM HH:mm z").withZone(snapshot.zone).format(snapshot.fetchedAt)),
        color = CardPalette.Muted,
    )
}

@Composable
private fun TimelineContent(snapshot: OutageSnapshot.Timeline, now: Instant, cardId: Long) {
    var tomorrow by rememberSaveable(cardId) { mutableStateOf(false) }
    var expanded by rememberSaveable(cardId) { mutableStateOf(false) }
    val date = now.atZone(snapshot.zone).toLocalDate().plusDays(if (tomorrow) 1 else 0)
    val status = snapshot.nowStatus(now)
    Text(when (status) {
        is NowStatus.PowerOn -> status.nextOutageAt?.let {
            stringResource(R.string.outage_next, DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(snapshot.zone).format(it))
        } ?: stringResource(R.string.outage_power_on)
        is NowStatus.PowerOff -> status.until?.let {
            stringResource(R.string.outage_power_off_until,
                (if (status.estimated) "≈ " else "") + DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(snapshot.zone).format(it))
        } ?: stringResource(R.string.outage_unknown_end)
        NowStatus.Unknown -> stringResource(R.string.outage_status_unknown)
    }, color = CardPalette.Title)
    ScheduleDaySwitch(showTomorrow = tomorrow, onTabSelected = { tomorrow = it })
    if (snapshot.schedulesSuspended) Text(stringResource(R.string.schedules_suspended), color = CardPalette.Off)
    val intervals = intervalsForDay(snapshot.intervals, date, snapshot.zone)
    val dayStart = date.atStartOfDay(snapshot.zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(snapshot.zone).toInstant()
    val covered = snapshot.knownCoverage.any { it.start < dayEnd && it.end > dayStart }
    if (covered || intervals.isNotEmpty()) IntervalTimeline(snapshot, date, now, intervals)
    if (intervals.isEmpty()) {
        Text(stringResource(if (covered) R.string.no_outages else R.string.day_missing), color = CardPalette.Muted)
    } else {
        // Keep the primary incident visible; the rest remain available to accessibility too.
        val ordered = intervals.sortedWith(compareBy<OutageInterval> {
            when {
                it.end?.let { end -> end <= now } == true -> 2
                it.start > now -> 1
                else -> 0
            }
        }
            .thenBy { it.distanceMeters ?: Double.MAX_VALUE }.thenBy { it.start })
        (if (expanded) ordered else ordered.take(1)).forEach { interval ->
            val original = snapshot.intervals.firstOrNull { it.kind == interval.kind && it.cause == interval.cause && it.distanceMeters == interval.distanceMeters && it.endIsEstimate == interval.endIsEstimate && it.start <= interval.start && (it.end?.let { value -> value >= (interval.end ?: dayEnd) } != false) }
                ?: interval
            val timeFormat = DateTimeFormatter.ofPattern("dd.MM HH:mm XXX").withZone(snapshot.zone)
            val end = original.end?.let { (if (original.endIsEstimate) "≈ " else "") + timeFormat.format(it) }
                ?: stringResource(R.string.outage_restoration_unspecified)
            Text(stringResource(R.string.outage_interval, kindLabel(interval.kind), timeFormat.format(original.start), end), color = CardPalette.Title)
            interval.cause?.let { Text(it, color = CardPalette.Muted, maxLines = 3, overflow = TextOverflow.Ellipsis) }
            interval.affectedCustomers?.let { Text(stringResource(R.string.outage_customers, it), color = CardPalette.Muted) }
        }
        if (ordered.size > 1) TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) stringResource(R.string.outage_collapse) else stringResource(R.string.outage_more, ordered.size - 1))
        }
    }
}

@Composable
@ReadOnlyComposable
private fun kindLabel(kind: OutageKind): String = stringResource(when (kind) {
    OutageKind.PLANNED -> R.string.outage_planned
    OutageKind.POSSIBLE -> R.string.outage_possible
    OutageKind.ACTUAL -> R.string.outage_actual
    OutageKind.EMERGENCY -> R.string.outage_emergency
})

@Composable
private fun IntervalTimeline(snapshot: OutageSnapshot.Timeline, date: LocalDate, now: Instant, intervals: List<OutageInterval>) {
    val locale = LocalConfiguration.current.locales[0]
    val start = date.atStartOfDay(snapshot.zone).toInstant()
    val end = date.plusDays(1).atStartOfDay(snapshot.zone).toInstant()
    val seconds = Duration.between(start, end).seconds.toFloat()
    fun fraction(time: Instant) = (Duration.between(start, time).seconds / seconds).coerceIn(0f, 1f)
    val off = CardPalette.Off
    val on = CardPalette.Yes
    val muted = CardPalette.Banner
    val marker = CardPalette.Title
    val labels = OutageKind.entries.associateWith { kindLabel(it) }
    val template = stringResource(R.string.outage_interval)
    val unspecified = stringResource(R.string.outage_restoration_unspecified)
    val timeFormat = DateTimeFormatter.ofPattern("dd.MM HH:mm XXX").withZone(snapshot.zone)
    val description = intervals.joinToString("; ") {
        String.format(locale, template, labels[it.kind], timeFormat.format(it.start),
            it.end?.let { end -> (if (it.endIsEstimate) "≈ " else "") + timeFormat.format(end) } ?: unspecified)
    }
    Column {
        Canvas(Modifier.fillMaxWidth().height(32.dp).semantics { contentDescription = description }) {
            drawRect(muted)
            snapshot.knownCoverage.forEach { coverage ->
                if (coverage.start < end && coverage.end > start) {
                    val left = fraction(coverage.start) * size.width
                    val right = fraction(coverage.end) * size.width
                    drawRect(on, Offset(left, 0f), Size(right - left, size.height))
                }
            }
            intervals.sortedBy { when (it.kind) {
                OutageKind.POSSIBLE -> 0
                OutageKind.PLANNED -> 1
                OutageKind.ACTUAL -> 2
                OutageKind.EMERGENCY -> 3
            } }.forEach { interval ->
                val left = fraction(interval.start) * size.width
                val right = fraction(interval.end ?: end) * size.width
                val color = if (interval.kind == OutageKind.EMERGENCY) Color(0xFFD55E00) else off
                if (interval.kind == OutageKind.POSSIBLE) {
                    drawRect(muted, Offset(left, 0f), Size(right - left, size.height))
                    clipRect(left, 0f, right, size.height) {
                        for (x in 0..((size.width + size.height) / 8.dp.toPx()).toInt()) {
                            val position = x * 8.dp.toPx()
                            drawLine(color, Offset(position, 0f), Offset(position - size.height, size.height), 2.dp.toPx())
                        }
                    }
                } else {
                    drawRect(color, Offset(left, 0f), Size(right - left, size.height))
                    if (interval.kind == OutageKind.EMERGENCY) {
                        for (x in 0..((right - left) / 10.dp.toPx()).toInt()) {
                            val position = left + x * 10.dp.toPx()
                            drawLine(Color.White, Offset(position, 8.dp.toPx()), Offset(position, 19.dp.toPx()), 2.dp.toPx())
                        }
                    }
                }
                if (interval.end == null) {
                    val tip = Offset(right - 2.dp.toPx(), size.height / 2)
                    drawLine(marker, tip - Offset(7.dp.toPx(), 7.dp.toPx()), tip, 2.dp.toPx())
                    drawLine(marker, tip, tip - Offset(7.dp.toPx(), -7.dp.toPx()), 2.dp.toPx())
                }
            }
            if (now >= start && now < end) {
                val x = fraction(now) * size.width
                drawLine(marker, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // Labels are placed along elapsed time, including 23/25-hour days.
            (0..4).forEach { index ->
                Text(if (index == 4) "24:00" else DateTimeFormatter.ofPattern("HH:mm").withZone(snapshot.zone)
                    .format(start.plusSeconds((seconds * index / 4).toLong())), color = CardPalette.Muted)
            }
        }
    }
}
