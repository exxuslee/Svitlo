package org.exxlexxlee.svitlo.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
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

private val ShowTomorrowKey = booleanPreferencesKey("widget_show_tomorrow")
private val ShowTomorrowParam = ActionParameters.Key<Boolean>("widget_show_tomorrow")

class ScheduleWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadUi()
        provideContent {
            val showTomorrow = currentState<Preferences>()[ShowTomorrowKey] == true
            ScheduleWidgetContent(data, showTomorrow)
        }
    }

    private suspend fun loadUi(): ScheduleWidgetUi {
        val koin = GlobalContext.getOrNull() ?: return ScheduleWidgetMapper.map(null, null, Clock.System)
        val selection = koin.get<ObserveSelection>()().first()
        val schedule = selection?.let { koin.get<ObserveSchedule>()(it).first() }
        val clock = koin.getOrNull<Clock>() ?: Clock.System
        return ScheduleWidgetMapper.map(selection, schedule, clock)
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

class SelectDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val showTomorrow = parameters[ShowTomorrowParam] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[ShowTomorrowKey] = showTomorrow
            }
        }
        ScheduleWidget().update(context, glanceId)
    }
}

@Composable
private fun ScheduleWidgetContent(data: ScheduleWidgetUi, showTomorrow: Boolean) {
    val openApp = actionStartActivity<MainActivity>()
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.card)
            .cornerRadius(24.dp)
            .padding(10.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Header(
                title = data.title,
                groupLabel = data.groupLabel,
                modifier = GlanceModifier.clickable(openApp),
            )
            Spacer(GlanceModifier.height(8.dp))
            val today = data.today
            if (today == null) {
                Text(
                    text = LocalContext.current.getString(R.string.widget_empty),
                    style = TextStyle(color = WidgetColors.muted, fontSize = 14.sp),
                    modifier = GlanceModifier.clickable(openApp),
                )
            } else {
                val day = if (showTomorrow) data.tomorrow else today
                if (day == null) {
                    Text(
                        text = LocalContext.current.getString(R.string.tomorrow_unavailable),
                        style = TextStyle(color = WidgetColors.muted, fontSize = 13.sp),
                        modifier = GlanceModifier.clickable(openApp),
                    )
                } else {
                    Timeline(
                        states = day.states,
                        nowHalfHourIndex = if (showTomorrow) null else data.nowHalfHourIndex,
                        modifier = GlanceModifier.clickable(openApp),
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                DaySwitch(showTomorrow = showTomorrow)
            }
        }
    }
}

@Composable
private fun Header(title: String, groupLabel: String, modifier: GlanceModifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun DaySwitch(showTomorrow: Boolean) {
    Row(
        modifier = GlanceModifier
            .background(WidgetColors.switchTrack)
            .cornerRadius(16.dp)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayChip(
            text = LocalContext.current.getString(R.string.today),
            selected = !showTomorrow,
            showTomorrow = false,
        )
        DayChip(
            text = LocalContext.current.getString(R.string.tomorrow),
            selected = showTomorrow,
            showTomorrow = true,
        )
    }
}

@Composable
private fun DayChip(text: String, selected: Boolean, showTomorrow: Boolean) {
    Box(
        modifier = GlanceModifier
            .background(if (selected) WidgetColors.switchSelected else WidgetColors.switchTrack)
            .cornerRadius(14.dp)
            .clickable(
                actionRunCallback<SelectDayAction>(
                    actionParametersOf(ShowTomorrowParam to showTomorrow),
                ),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (selected) WidgetColors.title else WidgetColors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
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
private fun Timeline(
    states: List<PowerState>,
    nowHalfHourIndex: Int?,
    modifier: GlanceModifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HourRow(states = states, startHour = 0, nowHalfHourIndex = nowHalfHourIndex)
        Spacer(GlanceModifier.height(3.dp))
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
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HourGroup(states, startHour, nowHalfHourIndex, GlanceModifier.defaultWeight())
        HourGroup(states, startHour + 6, nowHalfHourIndex, GlanceModifier.defaultWeight())
    }
}

@Composable
private fun HourGroup(
    states: List<PowerState>,
    startHour: Int,
    nowHalfHourIndex: Int?,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (hour in startHour until startHour + 6) {
            val first = states.getOrElse(hour * 2) { PowerState.UNKNOWN }
            val second = states.getOrElse(hour * 2 + 1) { PowerState.UNKNOWN }
            HourCell(
                hour = hour,
                first = first,
                second = second,
                marked = nowHalfHourIndex == hour * 2 || nowHalfHourIndex == hour * 2 + 1,
                modifier = GlanceModifier.defaultWeight(),
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
    modifier: GlanceModifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .background(if (marked) WidgetColors.nowRing else WidgetColors.card)
            .cornerRadius(4.dp)
            .padding(if (marked) 1.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(22.dp)
                .cornerRadius(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (first == second) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(colorFor(first)),
                ) {}
            } else {
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(colorFor(first)),
                    ) {}
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(colorFor(second)),
                    ) {}
                }
            }
            Text(
                text = hour.toString(),
                modifier = GlanceModifier.fillMaxWidth(),
                style = TextStyle(
                    color = labelColor(first, second),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun colorFor(state: PowerState): ColorProvider = when (state) {
    PowerState.ON -> WidgetColors.on
    PowerState.OFF -> WidgetColors.off
    PowerState.MAYBE -> WidgetColors.maybe
    PowerState.UNKNOWN -> WidgetColors.unknown
}

private fun labelColor(first: PowerState, second: PowerState): ColorProvider {
    val onDark = listOf(first, second).all { it == PowerState.OFF || it == PowerState.UNKNOWN }
    return if (onDark) WidgetColors.labelOnDark else WidgetColors.labelOnLight
}

@SuppressLint("RestrictedApi")
private object WidgetColors {
    val card = ColorProvider(Color(0xFF2B2B33))
    val title = ColorProvider(Color(0xFFF4F4F6))
    val muted = ColorProvider(Color(0xFFB5B5BD))
    val on = ColorProvider(Color(0xFFFFCC00))
    val off = ColorProvider(Color(0xFFE74C5C))
    val maybe = ColorProvider(Color(0xFFF0A202))
    val unknown = ColorProvider(Color(0xFF6B6B73))
    val groupChip = ColorProvider(Color(0xFF8EA2FF))
    val switchTrack = ColorProvider(Color(0xFF1E1E26))
    val switchSelected = ColorProvider(Color(0xFF3E3E4A))
    val nowRing = ColorProvider(Color(0xFFF4F4F6))
    val labelOnLight = ColorProvider(Color(0xFF1A1A1A))
    val labelOnDark = ColorProvider(Color(0xFFF4F4F6))
}
