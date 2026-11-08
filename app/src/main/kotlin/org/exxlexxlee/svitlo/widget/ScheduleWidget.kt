package org.exxlexxlee.svitlo.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.svitlo.MainActivity
import org.exxlexxlee.svitlo.R
import org.koin.core.context.GlobalContext

class ScheduleWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT, EXPANDED),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadUi()
        provideContent {
            ScheduleWidgetContent(data)
        }
    }

    private suspend fun loadUi(): ScheduleWidgetUi {
        val koin = GlobalContext.getOrNull() ?: return ScheduleWidgetMapper.map(null, null, Clock.System)
        val selection = koin.get<ObserveSelection>()().first()
        val schedule = selection?.let { koin.get<ObserveSchedule>()(it).first() }
        val clock = koin.getOrNull<Clock>() ?: Clock.System
        return ScheduleWidgetMapper.map(selection, schedule, clock)
    }

    companion object {
        val COMPACT = DpSize(250.dp, 110.dp)
        val EXPANDED = DpSize(250.dp, 180.dp)
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

@Composable
private fun ScheduleWidgetContent(data: ScheduleWidgetUi) {
    val showTomorrow = LocalSize.current.height >= ScheduleWidget.EXPANDED.height
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.card)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Header(title = data.title, groupLabel = data.groupLabel)
            Spacer(GlanceModifier.height(10.dp))
            val today = data.today
            if (today == null) {
                Text(
                    text = LocalContext.current.getString(R.string.widget_empty),
                    style = TextStyle(color = WidgetColors.muted, fontSize = 14.sp),
                )
            } else {
                DayBlock(
                    title = LocalContext.current.getString(R.string.today),
                    day = today,
                    nowHalfHourIndex = data.nowHalfHourIndex,
                )
                if (showTomorrow) {
                    Spacer(GlanceModifier.height(12.dp))
                    val tomorrow = data.tomorrow
                    if (tomorrow == null) {
                        Text(
                            text = LocalContext.current.getString(R.string.tomorrow_unavailable),
                            style = TextStyle(color = WidgetColors.muted, fontSize = 13.sp),
                        )
                    } else {
                        DayBlock(
                            title = LocalContext.current.getString(R.string.tomorrow),
                            day = tomorrow,
                            nowHalfHourIndex = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, groupLabel: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = WidgetColors.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (groupLabel.isNotBlank()) {
            Chip(text = groupLabel, background = WidgetColors.groupChip, textColor = WidgetColors.title)
        }
    }
}

@Composable
private fun Chip(
    text: String,
    background: ColorProvider,
    textColor: ColorProvider,
) {
    Box(
        modifier = GlanceModifier
            .background(background)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
private fun DayBlock(
    title: String,
    day: WidgetDayUi,
    nowHalfHourIndex: Int?,
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = WidgetColors.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = LocalContext.current.getString(R.string.widget_total, day.total),
                style = TextStyle(color = WidgetColors.total, fontSize = 13.sp),
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Timeline(states = day.states, nowHalfHourIndex = nowHalfHourIndex)
        Spacer(GlanceModifier.height(2.dp))
        HourLabels()
        if (day.ranges.isNotBlank()) {
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = day.ranges,
                style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun Timeline(states: List<PowerState>, nowHalfHourIndex: Int?) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (hour in 0 until 24) {
            val first = states.getOrElse(hour * 2) { PowerState.UNKNOWN }
            val second = states.getOrElse(hour * 2 + 1) { PowerState.UNKNOWN }
            HourCell(
                first = first,
                second = second,
                markedHalf = when (nowHalfHourIndex) {
                    hour * 2 -> 0
                    hour * 2 + 1 -> 1
                    else -> -1
                },
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun HourCell(
    first: PowerState,
    second: PowerState,
    markedHalf: Int,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (markedHalf >= 0) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_now),
                contentDescription = null,
                modifier = GlanceModifier.size(8.dp, 10.dp),
            )
        } else {
            Spacer(GlanceModifier.height(10.dp))
        }
        if (first == second) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .cornerRadius(5.dp)
                    .background(colorFor(first)),
            ) {}
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth().height(18.dp)) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(18.dp)
                        .cornerRadius(5.dp)
                        .background(colorFor(first)),
                ) {}
                Spacer(GlanceModifier.width(1.dp))
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(18.dp)
                        .cornerRadius(5.dp)
                        .background(colorFor(second)),
                ) {}
            }
        }
    }
}

@Composable
private fun HourLabels() {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (hour in 0 until 24) {
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hour % 2 == 0) hour.toString().padStart(2, '0') else "",
                    style = TextStyle(color = WidgetColors.label, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun colorFor(state: PowerState): ColorProvider = when (state) {
    PowerState.ON -> WidgetColors.on
    PowerState.OFF -> WidgetColors.off
    PowerState.MAYBE -> WidgetColors.maybe
    PowerState.UNKNOWN -> WidgetColors.unknown
}

@SuppressLint("RestrictedApi")
private object WidgetColors {
    val card = ColorProvider(Color(0xFF2B2B33))
    val title = ColorProvider(Color(0xFFF4F4F6))
    val muted = ColorProvider(Color(0xFFB5B5BD))
    val label = ColorProvider(Color(0xFF8E8E96))
    val total = ColorProvider(Color(0xFFE86B6B))
    val on = ColorProvider(Color(0xFF2ECC71))
    val off = ColorProvider(Color(0xFFE74C5C))
    val maybe = ColorProvider(Color(0xFFF0A202))
    val unknown = ColorProvider(Color(0xFF6B6B73))
    val groupChip = ColorProvider(Color(0xFF8EA2FF))
}
