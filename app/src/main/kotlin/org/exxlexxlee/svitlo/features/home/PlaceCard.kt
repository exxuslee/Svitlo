package org.exxlexxlee.svitlo.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.OutageWindows
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.AddressIcon
import org.exxlexxlee.svitlo.features.home.models.Event

@Composable
fun PlaceCard(
    card: PlaceCardUi,
    eventHandler: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { eventHandler(Event.Open(card.id)) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardPalette.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(card, eventHandler)
            card.error?.let { error ->
                Text(text = error, color = CardPalette.Off, fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayLabel(
                        prefix = stringResource(R.string.today),
                        ranges = card.todayRanges,
                    )
                    DayTimeline(
                        states = card.todayStates,
                        nowIndex = card.nowIndex,
                        showMarker = card.showNowMarker,
                    )
                }
                StatusColumn(card, modifier = Modifier.width(120.dp).padding(start = 8.dp))
            }
            DayLabel(
                prefix = stringResource(R.string.tomorrow),
                ranges = card.tomorrowRanges,
            )
            DayTimeline(
                states = card.tomorrowStates,
                nowIndex = 0,
                showMarker = false,
            )
            if (card.schedulesSuspended) {
                EmergencyNotice()
            }
        }
    }
}

@Composable
private fun CardHeader(card: PlaceCardUi, eventHandler: (Event) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = AddressIcon.fromId(card.iconId).image,
            contentDescription = null,
            tint = CardPalette.Title,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = card.title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = CardPalette.Title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.groupLabel,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CardPalette.Accent)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        CardMenu(onDelete = { eventHandler(Event.DeleteRequested(card.id)) })
    }
}

@Composable
private fun CardMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.card_menu),
                tint = CardPalette.Title,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun DayLabel(prefix: String, ranges: String?) {
    val value = when (ranges) {
        null -> stringResource(R.string.day_missing)
        "" -> stringResource(R.string.no_outages)
        else -> ranges
    }
    val valueColor = if (ranges.isNullOrEmpty()) CardPalette.Muted else CardPalette.Range
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = CardPalette.Muted, fontWeight = FontWeight.Medium)) {
                append("$prefix: ")
            }
            withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
        },
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
}

@Composable
private fun StatusColumn(card: PlaceCardUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(stateColor(card.currentState)),
        )
        if (card.schedulesSuspended) {
            Text(
                text = stringResource(R.string.schedules_suspended),
                modifier = Modifier.padding(top = 8.dp),
                color = CardPalette.Off,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
        val minutes = card.minutesUntilChange
        val target = card.countdownTarget
        if (minutes != null && target != null) {
            Text(
                text = OutageWindows.formatDuration(minutes),
                modifier = Modifier.padding(top = 6.dp),
                color = CardPalette.Title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    if (target == CountdownTarget.PowerOn) R.string.until_on else R.string.until_off,
                ),
                color = CardPalette.Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (card.currentState == PowerState.OFF) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = CardPalette.Off,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(28.dp),
            )
        }
    }
}

@Composable
private fun DayTimeline(
    states: List<PowerState>,
    nowIndex: Int,
    showMarker: Boolean,
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            if (showMarker) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = size.width * (nowIndex + 0.5f) / states.size
                    val half = 6.dp.toPx()
                    val path = Path().apply {
                        moveTo(center, size.height)
                        lineTo(center - half, 0f)
                        lineTo(center + half, 0f)
                        close()
                    }
                    drawPath(path, CardPalette.Marker)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            states.forEachIndexed { index, state ->
                val color = if (showMarker && index == nowIndex) CardPalette.Marker else stateColor(state)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
        HourLabels()
    }
}

@Composable
private fun HourLabels() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        listOf(0, 4, 8, 12, 16, 20).forEach { hour ->
            Text(
                text = hour.toString().padStart(2, '0'),
                modifier = Modifier.offset(x = maxWidth * (hour / 24f)),
                color = CardPalette.Muted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun EmergencyNotice() {
    val notice = stringResource(R.string.emergency_notice)
    val emphasis = stringResource(R.string.emergency_notice_emphasis)
    val start = notice.indexOf(emphasis)
    val annotated = buildAnnotatedString {
        if (start < 0) {
            append(notice)
        } else {
            append(notice.substring(0, start))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CardPalette.Title)) {
                append(emphasis)
            }
            append(notice.substring(start + emphasis.length))
        }
    }
    Text(
        text = annotated,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardPalette.Banner)
            .padding(12.dp),
        color = CardPalette.BannerText,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

private fun stateColor(state: PowerState?): Color = when (state) {
    PowerState.ON -> CardPalette.On
    PowerState.OFF -> CardPalette.Off
    PowerState.MAYBE -> CardPalette.Maybe
    PowerState.UNKNOWN, null -> CardPalette.Unknown
}

internal object CardPalette {
    val Screen = Color(0xFFF2F4F8)
    val Card = Color(0xFFFFFFFF)
    val Title = Color(0xFF1B2437)
    val Muted = Color(0xFF8B95A7)
    val Off = Color(0xFFE23B3B)
    val On = Color(0xFF2EAE4E)
    val Maybe = Color(0xFFF0A202)
    val Unknown = Color(0xFF2C3444)
    val Accent = Color(0xFF5B6CFF)
    val Marker = Color(0xFF2F6FED)
    val Banner = Color(0xFFF0F1F4)
    val BannerText = Color(0xFF6D7686)
    val Range = Color(0xFFE14D4D)
}
