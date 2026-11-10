package org.exxlexxlee.svitlo.widget

import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import org.exxlexxlee.svitlo.R

/**
 * RemoteViews-compatible frosted bubble for the home-screen widget.
 * Translucent shading, a highlight and a rim simulate glass; this does not blur
 * the launcher's wallpaper. The Compose backdrop effect cannot run in RemoteViews.
 * The loading layout uses the same drawable.
 */
fun GlanceModifier.widgetFrostedBubble(): GlanceModifier =
    background(ImageProvider(R.drawable.widget_card_background))
        .cornerRadius(R.dimen.widget_bubble_corner_radius)
