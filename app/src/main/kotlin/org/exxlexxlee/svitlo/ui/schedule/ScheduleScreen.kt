package org.exxlexxlee.svitlo.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import org.exxlexxlee.domain.model.Dial
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.ui.dial.OutageDial
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onChangeSelection: () -> Unit,
    viewModel: ScheduleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = state.selection?.let { "${it.region.name} · ${it.group.name}" }
        ?: stringResource(R.string.app_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    TextButton(onClick = onChangeSelection) {
                        Text(stringResource(R.string.change_selection))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!state.loaded || (state.dials == null && state.isRefreshing && state.error == null)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.updated_at,
                            formatUpdatedAt(state.schedule?.sourceUpdatedAt ?: state.schedule?.fetchedAt),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.isStale) {
                        Text(
                            text = stringResource(R.string.stale_data),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    state.error?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.dials == null) {
                        Text(stringResource(R.string.no_cached_schedule))
                    }
                    Legend()
                    state.dials?.let { dials ->
                        DialSection(
                            title = stringResource(R.string.today),
                            am = dials.todayAm,
                            pm = dials.todayPm,
                            isToday = true,
                            currentHour = state.currentHour,
                        )
                        if (dials.tomorrowAm != null && dials.tomorrowPm != null) {
                            DialSection(
                                title = stringResource(R.string.tomorrow),
                                am = dials.tomorrowAm,
                                pm = dials.tomorrowPm,
                                isToday = false,
                                currentHour = state.currentHour,
                            )
                        } else {
                            PlaceholderTomorrow()
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DialSection(
    title: String,
    am: Dial,
    pm: Dial,
    isToday: Boolean,
    currentHour: Int,
) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            OutageDial(
                hours = am.hours,
                hourOffset = 0,
                isToday = isToday,
                currentHour = currentHour,
            )
            Text(stringResource(R.string.hours_am), textAlign = TextAlign.Center)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            OutageDial(
                hours = pm.hours,
                hourOffset = 12,
                isToday = isToday,
                currentHour = currentHour,
            )
            Text(stringResource(R.string.hours_pm), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlaceholderTomorrow() {
    Text(text = stringResource(R.string.tomorrow), style = MaterialTheme.typography.titleLarge)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.tomorrow_unavailable),
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.legend_on), style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.legend_off), style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.legend_maybe), style = MaterialTheme.typography.labelMedium)
    }
}

private fun formatUpdatedAt(instant: Instant?): String {
    if (instant == null) return "—"
    val local = instant.toLocalDateTime(KyivTime.ZONE)
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.dayOfMonth}.${local.monthNumber}.${local.year} $hour:$minute"
}
