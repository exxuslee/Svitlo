package org.exxlexxlee.svitlo.features.address

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.models.AddressStep
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.ui.theme.AppColors
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme

@Composable
fun AddressContent(
    viewState: ViewState,
    eventHandler: (Event) -> Unit,
) {
    BackHandler(enabled = viewState.step != AddressStep.Region) {
        eventHandler(Event.Back)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        AddressHeader(
            step = viewState.step,
            isEditing = viewState.isEditing,
            onBack = { eventHandler(Event.Back) },
            onClose = { eventHandler(Event.Close) },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (viewState.isLoadingAreas && viewState.step == AddressStep.Region) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.Accent,
                )
            } else {
                when (viewState.step) {
                    AddressStep.Region -> RegionStep(viewState, eventHandler)
                    AddressStep.Supplier -> SupplierStep(viewState, eventHandler)
                    AddressStep.Group -> GroupStep(viewState, eventHandler)
                    AddressStep.Details -> DetailsStep(viewState, eventHandler)
                }
            }
        }
    }

    if (viewState.showAddressSearchHint) {
        AlertDialog(
            onDismissRequest = { eventHandler(Event.DismissAddressSearch) },
            text = { Text(stringResource(R.string.find_group_unavailable)) },
            confirmButton = {
                TextButton(onClick = { eventHandler(Event.DismissAddressSearch) }) {
                    Text(stringResource(R.string.got_it))
                }
            },
        )
    }

    if (viewState.showReminderPicker) {
        AlertDialog(
            onDismissRequest = { eventHandler(Event.DismissReminder) },
            title = { Text(stringResource(R.string.reminder_label)) },
            text = {
                Column {
                    listOf(0, 15, 30, 60).forEach { minutes ->
                        Text(
                            text = reminderText(minutes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { eventHandler(Event.ReminderSelected(minutes)) }
                                .padding(vertical = 12.dp),
                            color = AppColors.Title,
                            fontSize = 16.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { eventHandler(Event.DismissReminder) }) {
                    Text(stringResource(R.string.got_it))
                }
            },
        )
    }
}

@Composable
private fun AddressHeader(
    step: AddressStep,
    isEditing: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.back),
                    tint = AppColors.Title,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(if (isEditing) R.string.edit_address_title else R.string.address_title),
                modifier = Modifier.weight(1f),
                color = AppColors.Title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isEditing) {
                IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = AppColors.Title,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.address_step, step.index, stepTitle(step)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 14.dp),
            color = AppColors.Subtitle,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.Track),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(step.fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.Accent),
            )
        }
    }
}

@Composable
private fun stepTitle(step: AddressStep): String = when (step) {
    AddressStep.Region -> stringResource(R.string.step_region)
    AddressStep.Supplier -> stringResource(R.string.step_supplier)
    AddressStep.Group -> stringResource(R.string.step_group)
    AddressStep.Details -> stringResource(R.string.step_details)
}

@Composable
fun reminderText(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.reminder_never)
    minutes >= 60 && minutes % 60 == 0 -> stringResource(R.string.reminder_hours, minutes / 60)
    else -> stringResource(R.string.reminder_minutes, minutes)
}

@Preview
@Composable
private fun AddressContent_Preview() {
    SvitloTheme {
        AddressContent(
            viewState = ViewState(
                isLoadingAreas = false,
                areas = listOf(
                    ServiceArea(
                        id = "kyiv-region",
                        name = "Київська область",
                        suppliers = listOf(
                            Supplier(
                                region = Region(
                                    id = "kyiv-region",
                                    name = "ПрАТ «ДТЕК Київські регіональні електромережі»",
                                    providerId = "dtek",
                                ),
                                primary = true,
                            ),
                        ),
                    ),
                ),
            ),
            eventHandler = {},
        )
    }
}

@Preview
@Composable
private fun AddressContent_Dark_Preview() {
    SvitloTheme(darkTheme = true) {
        AddressContent(
            viewState = ViewState(
                isLoadingAreas = false,
                areas = listOf(
                    ServiceArea(
                        id = "kyiv-region",
                        name = "Київська область",
                        suppliers = listOf(
                            Supplier(
                                region = Region(
                                    id = "kyiv-region",
                                    name = "ПрАТ «ДТЕК Київські регіональні електромережі»",
                                    providerId = "dtek",
                                ),
                                primary = true,
                            ),
                        ),
                    ),
                ),
            ),
            eventHandler = {},
        )
    }
}
