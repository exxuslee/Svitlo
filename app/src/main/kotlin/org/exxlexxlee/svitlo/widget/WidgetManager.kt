package org.exxlexxlee.svitlo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

interface WidgetManager {
    fun hasInstalledWidgets(): Boolean
    fun isBannerDismissed(): Boolean
    fun setBannerDismissed(dismissed: Boolean)
    fun requestPinWidget(): Boolean
}

class WidgetManagerImpl(
    private val context: Context,
) : WidgetManager {
    private var bannerDismissed: Boolean = false

    override fun hasInstalledWidgets(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ScheduleWidgetReceiver::class.java)
        return appWidgetManager.getAppWidgetIds(componentName).isNotEmpty()
    }

    override fun isBannerDismissed(): Boolean = bannerDismissed

    override fun setBannerDismissed(dismissed: Boolean) {
        bannerDismissed = dismissed
    }

    override fun requestPinWidget(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val componentName = ComponentName(context, ScheduleWidgetReceiver::class.java)
            appWidgetManager.requestPinAppWidget(componentName, null, null)
            true
        } else {
            false
        }
    }
}

