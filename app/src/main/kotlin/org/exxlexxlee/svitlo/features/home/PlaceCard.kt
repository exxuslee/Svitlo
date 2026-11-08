package org.exxlexxlee.svitlo.features.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.OutageWindows
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.AddressIcon
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import kotlin.math.roundToInt

@Composable
fun PlaceCard(
    card: PlaceCardUi,
    eventHandler: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceCardSwipeable(
        card = card,
        eventHandler = eventHandler,
        modifier = modifier,
    )
}

@Composable
internal fun PlaceCardSwipeable(
    card: PlaceCardUi,
    eventHandler: (Event) -> Unit,
    modifier: Modifier = Modifier,
    initialOffset: Float = 0f,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealWidth = 88.dp
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val maxDragPx = with(density) { 120.dp.toPx() }
    val offsetX = remember(card.id) { Animatable(initialOffset) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val currentOffset = offsetX.value
        val revealProgress = (-currentOffset / revealWidthPx).coerceIn(0f, 1f)

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(revealWidth)
                    .graphicsLayer {
                        alpha = revealProgress
                        scaleX = 0.7f + (0.3f * revealProgress)
                        scaleY = 0.7f + (0.3f * revealProgress)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = CardPalette.Accent,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(44.dp),
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                            eventHandler(Event.Edit(card.id))
                        },
                        enabled = revealProgress > 0.5f,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = CardPalette.Off,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(44.dp),
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                            eventHandler(Event.DeleteRequested(card.id))
                        },
                        enabled = revealProgress > 0.5f,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + delta).coerceIn(-maxDragPx, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val target = when {
                                velocity < -500f -> -revealWidthPx
                                velocity > 500f -> 0f
                                offsetX.value < (-revealWidthPx / 2f) -> -revealWidthPx
                                else -> 0f
                            }
                            offsetX.animateTo(
                                targetValue = target,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                )
                .clickable {
                    if (offsetX.value != 0f) {
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    } else {
                        eventHandler(Event.Open(card.id))
                    }
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardPalette.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            var showTomorrow by remember(card.id) { mutableStateOf(false) }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CardHeader(card)

                card.error?.let { error ->
                    Text(text = error, color = CardPalette.Off, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DaySwitch(
                            showTomorrow = showTomorrow,
                            onTabSelected = { showTomorrow = it },
                        )
                        DayLabel(
                            prefix = if (showTomorrow) stringResource(R.string.tomorrow) else stringResource(
                                R.string.today
                            ),
                            ranges = if (showTomorrow) card.tomorrowRanges else card.todayRanges,
                        )
                    }
                    StatusColumn(
                        card = card,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 8.dp),
                    )
                }

                val currentStates = if (showTomorrow) card.tomorrowStates else card.todayStates
                val hasData = if (showTomorrow) {
                    card.tomorrowRanges != null || currentStates.any { it != PowerState.UNKNOWN }
                } else {
                    true
                }

                if (hasData) {
                    WidgetTimeline(
                        states = currentStates,
                        nowHalfHourIndex = if (!showTomorrow && card.showNowMarker) card.nowIndex else null,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardPalette.Banner),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.tomorrow_unavailable),
                            color = CardPalette.Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (card.schedulesSuspended) {
                    EmergencyNotice()
                }
            }
        }
    }
}

@Composable
private fun CardHeader(card: PlaceCardUi) {
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
                .clip(RoundedCornerShape(30))
                .background(CardPalette.Accent)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DaySwitch(
    showTomorrow: Boolean,
    onTabSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardPalette.Banner)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayChip(
            text = stringResource(R.string.today),
            selected = !showTomorrow,
            onClick = { onTabSelected(false) },
        )
        DayChip(
            text = stringResource(R.string.tomorrow),
            selected = showTomorrow,
            onClick = { onTabSelected(true) },
        )
    }
}

@Composable
private fun DayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) CardPalette.Accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else CardPalette.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
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
                .background(
                    if (card.currentState == PowerState.ON) CardPalette.Yes
                    else stateColor(card.currentState)
                ),
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
private fun WidgetTimeline(
    states: List<PowerState>,
    nowHalfHourIndex: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HourRow(states = states, startHour = 0, nowHalfHourIndex = nowHalfHourIndex)
        HourRow(states = states, startHour = 12, nowHalfHourIndex = nowHalfHourIndex)
    }
}

@Composable
private fun HourRow(
    states: List<PowerState>,
    startHour: Int,
    nowHalfHourIndex: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (hour in startHour until (startHour + 12)) {
            val first = states.getOrElse(hour * 2) { PowerState.UNKNOWN }
            val second = states.getOrElse((hour * 2) + 1) { PowerState.UNKNOWN }
            val isCurrentHour = nowHalfHourIndex != null && (nowHalfHourIndex / 2 == hour)
            HourCell(
                hour = hour,
                first = first,
                second = second,
                marked = isCurrentHour,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HourCell(
    hour: Int,
    first: PowerState,
    second: PowerState,
    marked: Boolean,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(outerShape)
            .then(
                if (marked) {
                    Modifier
                        .background(CardPalette.Marker)
                        .padding(1.5.dp)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(if (marked) RoundedCornerShape(2.5.dp) else outerShape),
            contentAlignment = Alignment.Center,
        ) {
            if (first == second) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(stateColor(first)),
                )
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(stateColor(first)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(stateColor(second)),
                    )
                }
            }
            Text(
                text = hour.toString(),
                color = hourLabelColor(first, second),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun hourLabelColor(first: PowerState, second: PowerState): Color {
    val onDark = listOf(first, second).all { it == PowerState.OFF || it == PowerState.UNKNOWN }
    return if (onDark) Color.White else Color(0xFF1B2437)
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
    val On = Color(0xFFFFE826)
    val Yes = Color(0xFF2EAE4E)
    val Maybe = Color(0xFF4DAF98)
    val Unknown = Color(0xFF2C3444)
    val Accent = Color(0xFF5B6CFF)
    val Marker = Color(0xFF2F6FED)
    val Banner = Color(0xFFF0F1F4)
    val BannerText = Color(0xFF6D7686)
    val Range = Color(0xFFE14D4D)
}

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F8)
@Composable
private fun PlaceCard_Normal_Preview() {
    val halves = MutableList(48) { PowerState.ON }
    for (index in 3 until 18) halves[index] = PowerState.OFF
    for (index in 26 until 41) halves[index] = PowerState.OFF
    halves[47] = PowerState.OFF
    SvitloTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlaceCard(
                card = PlaceCardUi(
                    id = 1,
                    title = "Київська область",
                    groupLabel = "Група 2.2",
                    iconId = "home",
                    todayRanges = "01:30–09:00, 13:00–20:30, 23:30–24:00",
                    tomorrowRanges = null,
                    todayStates = halves,
                    tomorrowStates = List(48) { PowerState.UNKNOWN },
                    showNowMarker = true,
                    nowIndex = 37,
                    currentState = PowerState.OFF,
                    minutesUntilChange = 154,
                    countdownTarget = CountdownTarget.PowerOn,
                    schedulesSuspended = false,
                    error = null,
                ),
                eventHandler = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, backgroundColor = 0xFFF2F4F8)
@Composable
private fun PlaceCard_Swiped_Preview() {
    val halves = MutableList(48) { PowerState.ON }
    for (index in 3 until 18) halves[index] = PowerState.OFF
    for (index in 26 until 41) halves[index] = PowerState.OFF
    halves[47] = PowerState.OFF
    val offsetPx = with(LocalDensity.current) { (-88).dp.toPx() }
    SvitloTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlaceCardSwipeable(
                card = PlaceCardUi(
                    id = 1,
                    title = "Київська область",
                    groupLabel = "Група 2.2",
                    iconId = "home",
                    todayRanges = "01:30–09:00, 13:00–20:30, 23:30–24:00",
                    tomorrowRanges = null,
                    todayStates = halves,
                    tomorrowStates = List(48) { PowerState.UNKNOWN },
                    showNowMarker = true,
                    nowIndex = 37,
                    currentState = PowerState.OFF,
                    minutesUntilChange = 154,
                    countdownTarget = CountdownTarget.PowerOn,
                    schedulesSuspended = false,
                    error = null,
                ),
                eventHandler = {},
                initialOffset = offsetPx,
            )
        }
    }
}
