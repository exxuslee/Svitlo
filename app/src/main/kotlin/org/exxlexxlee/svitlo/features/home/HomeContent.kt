package org.exxlexxlee.svitlo.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.features.home.models.ViewState
import org.exxlexxlee.svitlo.ui.schedule.CardPalette
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    viewState: ViewState,
    eventHandler: (Event) -> Unit,
) {
    val pending = viewState.cards.firstOrNull { it.id == viewState.pendingDeleteId }
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { eventHandler(Event.DismissDelete) },
            title = { Text(stringResource(R.string.delete_place_title)) },
            text = { Text(stringResource(R.string.delete_place_body, pending.title)) },
            confirmButton = {
                TextButton(onClick = { eventHandler(Event.ConfirmDelete) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { eventHandler(Event.DismissDelete) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = CardPalette.Screen,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { eventHandler(Event.Add) },
                containerColor = CardPalette.Accent,
                contentColor = Color.White,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_address),
                )
            }
        },
    ) { padding ->
        ColumnHeader(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            viewState = viewState,
            eventHandler = eventHandler,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnHeader(
    viewState: ViewState,
    eventHandler: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                color = CardPalette.Title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { eventHandler(Event.Settings) }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = CardPalette.Title,
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = viewState.isRefreshing,
            onRefresh = { eventHandler(Event.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                !viewState.loaded -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardPalette.Accent)
                    }
                }

                viewState.cards.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.home_empty),
                            modifier = Modifier.padding(32.dp),
                            color = CardPalette.Muted,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                            12.dp
                        ),
                    ) {
                        if (viewState.showWidgetBanner) {
                            item(key = "widget_banner") {
                                WidgetBanner(
                                    onAddWidget = { eventHandler(Event.AddWidgetClicked) },
                                    onDismiss = { eventHandler(Event.DismissWidgetBanner) },
                                )
                            }
                        }
                        items(viewState.cards, key = { it.id }) { card ->
                            PlaceCard(card = card, eventHandler = eventHandler)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun HomeContent_Preview() {
    val halves = MutableList(48) { PowerState.ON }
    for (index in 3 until 18) halves[index] = PowerState.OFF
    for (index in 26 until 41) halves[index] = PowerState.OFF
    halves[47] = PowerState.OFF
    SvitloTheme {
        HomeContent(
            viewState = ViewState(
                loaded = true,
                showWidgetBanner = true,
                cards = listOf(
                    PlaceCardUi(
                        id = 1,
                        title = "Київська область",
                        groupLabel = "Група 2.2",
                        iconId = "home",
                        todayRanges = "01:30–09:00, 13:00–20:30, 23:30–24:00",
                        tomorrowRanges = null,
                        todayStates = halves,
                        tomorrowStates = List(48) { PowerState.UNKNOWN },
                        showNowMarker = true,
                        nowIndex = 37,
                        currentState = PowerState.OFF,
                        minutesUntilChange = 154,
                        countdownTarget = CountdownTarget.PowerOn,
                        schedulesSuspended = true,
                        error = null,
                    ),
                ),
            ),
            eventHandler = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun HomeContent_Dark_Preview() {
    val halves = MutableList(48) { PowerState.ON }
    for (index in 3 until 18) halves[index] = PowerState.OFF
    for (index in 26 until 41) halves[index] = PowerState.OFF
    halves[47] = PowerState.OFF
    SvitloTheme(darkTheme = true) {
        HomeContent(
            viewState = ViewState(
                loaded = true,
                showWidgetBanner = true,
                cards = listOf(
                    PlaceCardUi(
                        id = 1,
                        title = "Київська область",
                        groupLabel = "Група 2.2",
                        iconId = "home",
                        todayRanges = "01:30–09:00, 13:00–20:30, 23:30–24:00",
                        tomorrowRanges = null,
                        todayStates = halves,
                        tomorrowStates = List(48) { PowerState.UNKNOWN },
                        showNowMarker = true,
                        nowIndex = 37,
                        currentState = PowerState.OFF,
                        minutesUntilChange = 154,
                        countdownTarget = CountdownTarget.PowerOn,
                        schedulesSuspended = true,
                        error = null,
                    ),
                ),
            ),
            eventHandler = {},
        )
    }
}
