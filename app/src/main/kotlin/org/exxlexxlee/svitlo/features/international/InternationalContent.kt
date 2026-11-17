package org.exxlexxlee.svitlo.features.international

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.AddressHeader
import org.exxlexxlee.svitlo.features.address.AddressIcon
import org.exxlexxlee.svitlo.features.address.DetailsStep
import org.exxlexxlee.svitlo.features.address.ReminderPicker
import org.exxlexxlee.svitlo.features.address.models.ViewState as AddressViewState
import org.exxlexxlee.svitlo.features.international.models.Event
import org.exxlexxlee.svitlo.features.international.models.InternationalStep
import org.exxlexxlee.svitlo.features.international.models.ViewState
import org.exxlexxlee.svitlo.features.settings.countryNameRes
import org.exxlexxlee.svitlo.ui.theme.AppColors

@Composable
fun InternationalContent(
    state: ViewState,
    eventHandler: (Event) -> Unit,
) {
    val back = { eventHandler(Event.Back) }
    BackHandler(onBack = back)
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        AddressHeader(
            stepIndex = state.stepIndex,
            stepCount = state.stepCount,
            stepTitle = stringResource(state.step.titleRes()),
            isEditing = state.isEditing,
            onBack = back,
            onClose = { eventHandler(Event.Close) },
        )
        if (state.step == InternationalStep.Details) {
            DetailsStep(
                viewState = AddressViewState(
                    addressName = state.details.name,
                    reminderMinutes = state.details.reminderMinutes,
                    quietNight = state.details.quietNight,
                    icon = AddressIcon.fromId(state.details.iconId),
                    error = if (state.failed) stringResource(R.string.international_save_error) else null,
                ),
                eventHandler = { eventHandler(Event.DetailsChanged(it)) },
                canSave = state.canSave,
                notice = listOfNotNull(
                    stringResource(state.country.countryNameRes()),
                    state.source?.name,
                    state.query.takeIf { it.isNotBlank() },
                    state.source?.let { stringResource(it.access.descriptionRes()) },
                    stringResource(R.string.international_reminders_note),
                ).joinToString("\n\n"),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(state.country.countryNameRes()),
                        color = AppColors.Body,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        stringResource(state.step.hintRes()),
                        modifier = Modifier.padding(top = 8.dp),
                        color = AppColors.Title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.step == InternationalStep.Supplier) {
                    item {
                        Text(
                            stringResource(R.string.international_coverage),
                            color = AppColors.Body
                        )
                    }
                    items(state.sources, key = { it.id }) { source ->
                        ChoiceRow(
                            source.name,
                            stringResource(source.access.descriptionRes())
                        ) { eventHandler(Event.SourceSelected(source)) }
                    }
                } else {
                    state.source?.let { source ->
                        item {
                            Text(
                                source.name,
                                color = AppColors.Body,
                                style = MaterialTheme.typography.labelLarge
                            )
                            val uriHandler = LocalUriHandler.current
                            TextButton(onClick = { uriHandler.openUri(source.website) }) {
                                Text(stringResource(R.string.international_open_source))
                            }
                        }
                    }
                    when (state.step) {
                        InternationalStep.Location -> {
                            item {
                                OutlinedTextField(
                                    state.query, { eventHandler(Event.QueryChanged(it)) },
                                    label = { Text(stringResource(R.string.international_filter)) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                )
                                TextButton(onClick = { eventHandler(Event.Refresh) }, enabled = !state.loading) {
                                    Text(stringResource(R.string.international_refresh))
                                }
                            }
                            val locations =
                                state.outages.map { it.location }.filter { it.isNotBlank() }
                                    .distinct()
                                    .filter { it.contains(state.query.trim(), ignoreCase = true) }
                                    .sorted()
                            items(locations, key = { it }) { location ->
                                ChoiceRow(location) { eventHandler(Event.LocationSelected(location)) }
                            }
                            if (state.loaded && locations.isEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.international_empty),
                                        color = AppColors.Body
                                    )
                                }
                            }
                        }

                        InternationalStep.City, InternationalStep.Street -> {
                            val street = state.step == InternationalStep.Street
                            item {
                                if (street && state.cityQuery.isNotBlank()) Text(
                                    state.cityQuery,
                                    color = AppColors.Body
                                )
                                val query = if (street) state.streetQuery else state.cityQuery
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { eventHandler(if (street) Event.StreetQueryChanged(it) else Event.CityQueryChanged(it)) },
                                    label = { Text(stringResource(if (street) R.string.international_street else R.string.international_city)) },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                )
                                TextButton(
                                    onClick = { eventHandler(Event.Search) },
                                    enabled = !state.loading && query.trim().length >= 3
                                ) {
                                    Text(stringResource(R.string.international_search))
                                }
                            }
                            items(state.options, key = { it.id }) { option ->
                                ChoiceRow(option.name) { eventHandler(Event.OptionSelected(option)) }
                            }
                            if (state.loaded && state.options.isEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.international_search_empty),
                                        color = AppColors.Body
                                    )
                                }
                            }
                        }

                        InternationalStep.House -> item {
                            Text(
                                listOf(
                                    state.cityQuery,
                                    state.streetQuery
                                ).filter { it.isNotBlank() }.joinToString(" · "),
                                color = AppColors.Body
                            )
                            OutlinedTextField(
                                state.house,
                                { eventHandler(Event.HouseChanged(it)) },
                                label = { Text(stringResource(R.string.international_house)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                singleLine = true,
                            )
                        }

                        else -> Unit
                    }
                    if (state.loading) item {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth(),
                            color = AppColors.Accent
                        )
                    }
                    if (state.failed) item {
                        Text(
                            stringResource(R.string.international_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            if (state.step in listOf(InternationalStep.Location, InternationalStep.House)) {
                Button(
                    onClick = { eventHandler(Event.Next) }, enabled = state.canContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Accent,
                        contentColor = Color.White,
                        disabledContainerColor = AppColors.Banner,
                        disabledContentColor = AppColors.Body,
                    ),
                ) { Text(stringResource(R.string.address_next), fontWeight = FontWeight.Bold) }
            }
        }
    }
    if (state.showReminderPicker) ReminderPicker { eventHandler(Event.DetailsChanged(it)) }
}

@Composable
private fun ChoiceRow(title: String, description: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick, color = AppColors.Card, contentColor = AppColors.Title,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                description?.let {
                    Text(
                        it,
                        color = AppColors.Body,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColors.Accent)
        }
    }
}

private fun InternationalStep.titleRes(): Int = when (this) {
    InternationalStep.Supplier -> R.string.step_supplier
    InternationalStep.Location -> R.string.international_step_location
    InternationalStep.City -> R.string.international_city
    InternationalStep.Street -> R.string.international_street
    InternationalStep.House -> R.string.international_house
    InternationalStep.Details -> R.string.step_details
}

private fun InternationalStep.hintRes(): Int = when (this) {
    InternationalStep.Supplier -> R.string.international_choose_supplier
    InternationalStep.Location -> R.string.international_choose_location
    InternationalStep.City -> R.string.international_choose_city
    InternationalStep.Street -> R.string.international_choose_street
    InternationalStep.House -> R.string.international_enter_house
    InternationalStep.Details -> R.string.step_details
}

private fun SourceAccess.descriptionRes(): Int = when (this) {
    SourceAccess.LIVE -> R.string.international_live
    SourceAccess.ADDRESS -> R.string.international_address
    SourceAccess.ARCHIVED -> R.string.international_archived
    SourceAccess.PERMISSION_REQUIRED -> R.string.international_permission
    SourceAccess.UNAVAILABLE -> R.string.international_unavailable
    SourceAccess.WEBSITE -> R.string.international_website
}
