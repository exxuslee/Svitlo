package org.exxlexxlee.svitlo.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.svitlo.MainActivity
import org.exxlexxlee.svitlo.R
import org.koin.core.context.GlobalContext

class ScheduleWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) = coroutineScope {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        WidgetDaySelection.migrate(context, id, appWidgetId)
        val koin = GlobalContext.getOrNull()
        if (koin == null) {
            provideContent {
                ScheduleWidgetContent(ScheduleWidgetMapper.map(null, null, Clock.System), appWidgetId)
            }
        } else {
            // Share the initial load and live observation instead of subscribing to Room twice.
            val ui = observeWidgetUi(
                koin.get<ObserveSelection>(),
                koin.get<ObserveSchedule>(),
                koin.getOrNull<Clock>() ?: Clock.System,
            ).shareIn(this, SharingStarted.Eagerly, replay = 1)
            val initial = ui.first()
            provideContent {
                val data by ui.collectAsState(initial)
                ScheduleWidgetContent(data, appWidgetId)
            }
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        WidgetDaySelection.remove(context, GlanceAppWidgetManager(context).getAppWidgetId(glanceId))
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

/** Handles PendingIntents left in a launcher by an older installed version. */
class SelectDayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val tomorrow = parameters[ActionParameters.Key<Boolean>("widget_show_tomorrow")] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetDaySelection.save(context, appWidgetId, tomorrow)
        ScheduleWidget().update(context, glanceId)
    }
}

@Composable
internal fun ScheduleWidgetContent(data: ScheduleWidgetUi, appWidgetId: Int) {
    val openApp = actionStartActivity<MainActivity>()
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_card_background))
            .cornerRadius(28.dp)
            .padding(12.dp),
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
                val context = LocalContext.current
                val showTomorrow = WidgetDaySelection.read(context, appWidgetId)
                // Both pages are sent to the launcher once. A day click only changes visibility.
                AndroidRemoteViews(
                    remoteViews = RemoteViews(context.packageName, R.layout.schedule_widget_days),
                    containerViewId = R.id.widget_days,
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    DayPanel(data, appWidgetId, showTomorrow = false, visible = !showTomorrow)
                    DayPanel(data, appWidgetId, showTomorrow = true, visible = showTomorrow)
                }
            }
        }
    }
}

@Composable
private fun DayPanel(
    data: ScheduleWidgetUi,
    appWidgetId: Int,
    showTomorrow: Boolean,
    visible: Boolean,
) {
    val context = LocalContext.current
    val layout = if (showTomorrow) R.layout.schedule_widget_tomorrow else R.layout.schedule_widget_today
    val panel = if (showTomorrow) R.id.widget_tomorrow_panel else R.id.widget_today_panel
    AndroidRemoteViews(
        remoteViews = RemoteViews(context.packageName, layout).apply {
            setViewVisibility(panel, if (visible) View.VISIBLE else View.GONE)
        },
        containerViewId = panel,
        modifier = GlanceModifier.fillMaxWidth(),
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            val day = if (showTomorrow) data.tomorrow else data.today
            if (day == null) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(47.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = context.getString(R.string.tomorrow_unavailable),
                        style = TextStyle(
                            color = WidgetColors.muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            } else {
                Timeline(
                    states = day.states,
                    nowHalfHourIndex = if (showTomorrow) null else data.nowHalfHourIndex,
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            DaySwitch(appWidgetId, showTomorrow)
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
            Chip(
                text = groupLabel,
                background = WidgetColors.groupChip,
                textColor = WidgetColors.groupChipText,
            )
        }
    }
}

@Composable
private fun DaySwitch(appWidgetId: Int, showTomorrow: Boolean) {
    Row(
        modifier = GlanceModifier
            .background(WidgetColors.switchTrack)
            .cornerRadius(16.dp)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayChip(
            appWidgetId = appWidgetId,
            text = LocalContext.current.getString(R.string.today),
            selected = !showTomorrow,
            showTomorrow = false,
        )
        DayChip(
            appWidgetId = appWidgetId,
            text = LocalContext.current.getString(R.string.tomorrow),
            selected = showTomorrow,
            showTomorrow = true,
        )
    }
}

@Composable
private fun DayChip(appWidgetId: Int, text: String, selected: Boolean, showTomorrow: Boolean) {
    val click = if (selected) GlanceModifier else GlanceModifier.clickable(
        actionSendBroadcast(WidgetDayReceiver.intent(LocalContext.current, appWidgetId, showTomorrow)),
    )
    Box(
        modifier = GlanceModifier
            .background(if (selected) WidgetColors.switchSelected else WidgetColors.transparent)
            .cornerRadius(14.dp)
            .then(click)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = if (selected) WidgetColors.title else WidgetColors.muted,
                fontSize = 14.sp,
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
        for (hour in startHour until (startHour + 6)) {
            val first = states.getOrElse(hour * 2) { PowerState.UNKNOWN }
            val second = states.getOrElse((hour * 2) + 1) { PowerState.UNKNOWN }
            HourCell(
                hour = hour,
                first = first,
                second = second,
                marked = nowHalfHourIndex == hour * 2 || nowHalfHourIndex == (hour * 2) + 1,
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
            .background(if (marked) WidgetColors.nowRing else WidgetColors.cellBackground)
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
                    fontSize = 10.sp,
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
    val transparent = ColorProvider(Color.Transparent)
    val cellBackground = ColorProvider(Color.Transparent)
    val title = ColorProvider(Color(0xFFF4F6FC))
    val muted = ColorProvider(Color(0xFFBAC0D0))
    val on = ColorProvider(Color(0xFFFFCC00))
    val off = ColorProvider(Color(0xFFE74C5C))
    val maybe = ColorProvider(Color(0xFFF0A202))
    val unknown = ColorProvider(Color(0xFF55596B))
    val groupChip = ColorProvider(Color(0x338EA2FF))
    val groupChipText = ColorProvider(Color(0xFFD4DEFF))
    val switchTrack = ColorProvider(Color(0x33000000))
    val switchSelected = ColorProvider(Color(0x33FFFFFF))
    val nowRing = ColorProvider(Color(0xFFFFFFFF))
    val labelOnLight = ColorProvider(Color(0xFF1A1A1A))
    val labelOnDark = ColorProvider(Color(0xFFF4F6FC))
}
