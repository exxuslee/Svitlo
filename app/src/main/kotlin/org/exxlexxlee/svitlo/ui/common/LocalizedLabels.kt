package org.exxlexxlee.svitlo.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.exxlexxlee.svitlo.R

internal fun Context.localizedGroupLabel(label: String): String {
    val number = label.trim().replace(Regex("^(Група|Черга|Группа|Group)\\s+", RegexOption.IGNORE_CASE), "")
    return getString(R.string.group_label, number)
}

@Composable
internal fun localizedGroupLabel(label: String): String =
    LocalContext.current.localizedGroupLabel(label)

@Composable
internal fun localizedDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> stringResource(R.string.duration_hours_minutes, hours, rest)
        hours > 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_minutes, rest)
    }
}
