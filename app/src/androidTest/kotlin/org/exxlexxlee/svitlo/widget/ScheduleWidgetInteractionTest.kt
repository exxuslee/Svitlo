package org.exxlexxlee.svitlo.widget

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.View
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Parcel
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class ScheduleWidgetInteractionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val widgetId = 91234
    private val ui = ScheduleWidgetUi(
        title = "Дім", groupLabel = "Група 3.1",
        today = WidgetDayUi(List(48) { PowerState.ON }),
        tomorrow = WidgetDayUi(List(48) { PowerState.OFF }),
        nowHalfHourIndex = 20,
    )

    @After
    fun cleanUp() {
        WidgetDaySelection.remove(context, widgetId)
        WidgetDaySelection.remove(context, widgetId + 1)
    }

    @Test
    fun switchesBothWaysWithoutRecomposingOrRecreatingThePages() = runBlocking {
        WidgetDaySelection.save(context, widgetId, false)
        val views = GlanceRemoteViews().compose(context, DpSize(320.dp, 150.dp)) {
            ScheduleWidgetContent(ui, widgetId)
        }.remoteViews.forHost()

        instrumentation.runOnMainSync {
            val root = views.apply(context, FrameLayout(context))
            val today = root.findViewById<View>(R.id.widget_today_panel)
            val tomorrow = root.findViewById<View>(R.id.widget_tomorrow_panel)
            assertEquals(View.VISIBLE, today.visibility)
            assertEquals(View.GONE, tomorrow.visibility)
            renderSnapshot(root, "today")
            assertTrue(today.height > 0)

            WidgetDayReceiver.dayVisibility(context, true).reapply(context, root)
            assertEquals(View.GONE, today.visibility)
            assertEquals(View.VISIBLE, tomorrow.visibility)
            renderSnapshot(root, "tomorrow")
            assertTrue(tomorrow.height > 0)
            WidgetDayReceiver.dayVisibility(context, false).reapply(context, root)
            assertEquals(View.VISIBLE, today.visibility)
            assertEquals(View.GONE, tomorrow.visibility)
            assertSame(today, root.findViewById(R.id.widget_today_panel))
            assertSame(tomorrow, root.findViewById(R.id.widget_tomorrow_panel))
        }
    }

    @Test
    fun selectedDaySurvivesFullRenderAndIsIndependentForEachWidget() = runBlocking {
        WidgetDaySelection.save(context, widgetId, true)
        WidgetDaySelection.save(context, widgetId + 1, false)
        assertTrue(WidgetDaySelection.read(context, widgetId))
        assertFalse(WidgetDaySelection.read(context, widgetId + 1))
        val views = GlanceRemoteViews().compose(context, DpSize(320.dp, 150.dp)) {
            ScheduleWidgetContent(ui, widgetId)
        }.remoteViews.forHost()
        instrumentation.runOnMainSync {
            val root = views.apply(context, FrameLayout(context))
            assertEquals(View.GONE, root.findViewById<View>(R.id.widget_today_panel).visibility)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_tomorrow_panel).visibility)
        }
    }

    @Test
    fun unavailableTomorrowCanBeShownAndSwitchedBack() = runBlocking {
        val views = GlanceRemoteViews().compose(context, DpSize(320.dp, 150.dp)) {
            ScheduleWidgetContent(ui.copy(tomorrow = null), widgetId)
        }.remoteViews.forHost()
        instrumentation.runOnMainSync {
            val root = views.apply(context, FrameLayout(context))
            WidgetDayReceiver.dayVisibility(context, true).reapply(context, root)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_tomorrow_panel).visibility)
            renderSnapshot(root, "tomorrow-unavailable")
            WidgetDayReceiver.dayVisibility(context, false).reapply(context, root)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_today_panel).visibility)
        }
    }

    @Test
    fun dayBroadcastUpdatesSystemCacheForANewHostWithoutGlanceRendering() = runBlocking {
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 91235)
        val id = host.allocateAppWidgetId()
        try {
            instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
            try {
                assertTrue(manager.bindAppWidgetIdIfAllowed(
                    id, ComponentName(context, ScheduleWidgetReceiver::class.java),
                ))
            } finally {
                instrumentation.uiAutomation.dropShellPermissionIdentity()
            }
            val views = GlanceRemoteViews().compose(context, DpSize(320.dp, 150.dp)) {
                ScheduleWidgetContent(ui, id)
            }.remoteViews
            instrumentation.runOnMainSync {
                manager.updateAppWidget(id, views)
                val receiver = WidgetDayReceiver()
                receiver.onReceive(context, WidgetDayReceiver.intent(context, id, true))
                val tomorrowHost = host.createView(context, id, manager.getAppWidgetInfo(id))
                assertEquals(View.VISIBLE, tomorrowHost.findViewById<View>(R.id.widget_tomorrow_panel).visibility)
                assertEquals(View.GONE, tomorrowHost.findViewById<View>(R.id.widget_today_panel).visibility)
                assertTrue(WidgetDaySelection.read(context, id))

                receiver.onReceive(context, WidgetDayReceiver.intent(context, id, false))
                val todayHost = host.createView(context, id, manager.getAppWidgetInfo(id))
                assertEquals(View.VISIBLE, todayHost.findViewById<View>(R.id.widget_today_panel).visibility)
                assertEquals(View.GONE, todayHost.findViewById<View>(R.id.widget_tomorrow_panel).visibility)
                assertFalse(WidgetDaySelection.read(context, id))
            }
        } finally {
            host.deleteHost()
            WidgetDaySelection.remove(context, id)
        }
    }

    // RemoteViews cross Binder on a real launcher; include that serialization boundary in the test.
    private fun RemoteViews.forHost(): RemoteViews {
        val parcel = Parcel.obtain()
        return try {
            writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            RemoteViews.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    private fun renderSnapshot(root: View, name: String) {
        val density = context.resources.displayMetrics.density
        val width = (320 * density).toInt()
        val height = (150 * density).toInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            root.draw(Canvas(bitmap))
            File(context.getExternalFilesDir(null), "widget-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally {
            bitmap.recycle()
        }
    }
}
