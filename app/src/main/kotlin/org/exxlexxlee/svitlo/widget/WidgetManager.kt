package org.exxlexxlee.svitlo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit

interface WidgetManager {
    fun hasInstalledWidgets(): Boolean
    fun isBannerDismissed(): Boolean
    fun setBannerDismissed(dismissed: Boolean)
    fun requestPinWidget(): Boolean
}

class WidgetManagerImpl(
    private val context: Context,
) : WidgetManager {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasInstalledWidgets(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ScheduleWidgetReceiver::class.java)
        return appWidgetManager.getAppWidgetIds(componentName).isNotEmpty()
    }

    override fun isBannerDismissed(): Boolean {
        return prefs.getBoolean(KEY_BANNER_DISMISSED, false)
    }

    override fun setBannerDismissed(dismissed: Boolean) {
        prefs.edit { putBoolean(KEY_BANNER_DISMISSED, dismissed) }
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

    companion object {
        private const val PREFS_NAME = "svitlo_settings"
        private const val KEY_BANNER_DISMISSED = "widget_banner_dismissed"
    }
}
