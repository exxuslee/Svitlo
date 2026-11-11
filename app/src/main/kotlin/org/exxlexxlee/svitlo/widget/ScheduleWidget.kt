package org.exxlexxlee.svitlo.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.svitlo.MainActivity
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.ui.common.localizedGroupLabel
import org.exxlexxlee.svitlo.ui.schedule.DarkSchedulePalette
import org.exxlexxlee.svitlo.ui.schedule.GlanceScheduleDaySwitch
import org.exxlexxlee.svitlo.ui.schedule.GlanceScheduleGroupBadge
import org.exxlexxlee.svitlo.ui.schedule.GlanceScheduleTimeline
import org.exxlexxlee.svitlo.ui.schedule.glanceColor
import org.koin.core.context.GlobalContext

class ScheduleWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) = coroutineScope {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        WidgetDaySelection.migrate(context, id, appWidgetId)
        val koin = GlobalContext.getOrNull()
        if (koin == null) {
            provideContent {
                ScheduleWidgetContent(
                    ScheduleWidgetMapper.map(null, null, Clock.System),
                    appWidgetId
                )
            }
        } else {
            // Share the initial load and live observation instead of subscribing to Room twice.
            val ui = observeWidgetUi(
                koin.get<ObservePlaceSchedules>(),
                WidgetPlaceSelection.observe(context, appWidgetId),
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
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        WidgetDaySelection.remove(context, appWidgetId)
        WidgetPlaceSelection.remove(context, appWidgetId)
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

/** Handles PendingIntents left in a launcher by an older installed version. */
class SelectDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
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
        modifier = GlanceModifier.fillMaxSize(),
    ) {
        // Keep the card at content height even when the launcher allocates extra rows.
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .appWidgetBackground()
                .widgetFrostedBubble()
                .padding(12.dp),
        ) {
            val today = data.today
            if (today == null) {
                Text(
                    text = LocalContext.current.getString(R.string.widget_empty),
                    style = TextStyle(color = glanceColor(DarkSchedulePalette.Muted), fontSize = 14.sp),
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
    val layout =
        if (showTomorrow) R.layout.schedule_widget_tomorrow else R.layout.schedule_widget_today
    val panel = if (showTomorrow) R.id.widget_tomorrow_panel else R.id.widget_today_panel
    AndroidRemoteViews(
        remoteViews = RemoteViews(context.packageName, layout).apply {
            setViewVisibility(panel, if (visible) View.VISIBLE else View.GONE)
        },
        containerViewId = panel,
        modifier = GlanceModifier.fillMaxWidth(),
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlanceScheduleDaySwitch(showTomorrow) { tomorrow ->
                    actionSendBroadcast(WidgetDayReceiver.intent(context, appWidgetId, tomorrow))
                }
                Spacer(GlanceModifier.defaultWeight())
                if (data.groupLabel.isNotBlank()) {
                    GlanceScheduleGroupBadge(
                        text = context.localizedGroupLabel(data.groupLabel),
                        currentState = data.currentState,
                        modifier = GlanceModifier.clickable(actionRunCallback<NextPlaceAction>()),
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
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
                            color = glanceColor(DarkSchedulePalette.Title),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            } else {
                GlanceScheduleTimeline(
                    states = day.states,
                    nowHalfHourIndex = if (showTomorrow) null else data.nowHalfHourIndex,
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                )
            }
        }
    }
}
