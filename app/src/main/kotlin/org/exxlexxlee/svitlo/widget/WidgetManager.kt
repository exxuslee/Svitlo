package org.exxlexxlee.svitlo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

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

    override fun hasInstalledWidgets(): Boolean =
        installedScheduleWidgetIds(context).isNotEmpty()

    override fun isBannerDismissed(): Boolean = bannerDismissed

    override fun setBannerDismissed(dismissed: Boolean) {
        bannerDismissed = dismissed
    }

    override fun requestPinWidget(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return if (appWidgetManager.isRequestPinAppWidgetSupported && !isLegacyPocoLauncher()) {
            val componentName = ComponentName(context, ScheduleWidgetReceiver::class.java)
            appWidgetManager.requestPinAppWidget(componentName, null, null)
        } else {
            false
        }
    }

    private fun isLegacyPocoLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launcher = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo ?: return false
        if (launcher.packageName != "com.mi.android.globallauncher") return false

        val version = try {
            context.packageManager.getPackageInfo(launcher.packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            return false
        }
        // POCO Launcher 2 advertises pin support, but its AddItemActivity only handles shortcuts
        // (REQUEST_TYPE_SHORTCUT) and immediately finishes for widget requests.
        return version?.startsWith("2.") == true
    }
}

