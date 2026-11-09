package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.features.address.models.detailLabel
import org.exxlexxlee.svitlo.ui.theme.AppColors

private val FieldShape = RoundedCornerShape(16.dp)


@Composable
fun DetailsStep(viewState: ViewState, eventHandler: (Event) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            viewState.selectedGroup?.let { group ->
                Text(
                    text = stringResource(R.string.address_group, group.detailLabel()),
                    color = AppColors.Subtitle,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(16.dp))
            }
            FieldLabel(stringResource(R.string.address_name_label))
            TextField(
                value = viewState.addressName,
                onValueChange = { eventHandler(Event.AddressNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                colors = fieldColors(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.reminder_label))
            Text(
                text = reminderText(viewState.reminderMinutes),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .background(AppColors.Card)
                    .clickable { eventHandler(Event.OpenReminder) }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                color = AppColors.Title,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = viewState.quietNight,
                    onCheckedChange = { eventHandler(Event.QuietNightChanged(it)) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppColors.Accent,
                        uncheckedColor = AppColors.Subtitle,
                    ),
                )
                Text(
                    text = stringResource(R.string.quiet_night),
                    color = AppColors.Title,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            FieldLabel(stringResource(R.string.address_icon_label))
            IconPicker(selected = viewState.icon) { eventHandler(Event.IconSelected(it)) }
            viewState.error?.let { error ->
                Text(
                    text = error,
                    modifier = Modifier.padding(top = 12.dp),
                    color = AppColors.Title,
                )
            }
        }
        Button(
            onClick = { eventHandler(Event.Save) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            enabled = viewState.canSave,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent,
                contentColor = Color.White,
                disabledContainerColor = AppColors.Banner,
                disabledContentColor = AppColors.Subtitle,
            ),
        ) {
            Text(
                text = stringResource(R.string.save),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 8.dp),
        color = AppColors.Subtitle,
        fontSize = 15.sp,
    )
}

@Composable
private fun IconPicker(selected: AddressIcon, onSelect: (AddressIcon) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AddressIcon.picker.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { icon ->
                    val isSelected = icon == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .shadow(1.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) AppColors.AccentSoft else AppColors.Card)
                            .border(1.5.dp, AppColors.Accent, RoundedCornerShape(16.dp))
                            .clickable { onSelect(icon) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon.image,
                            contentDescription = icon.id,
                            tint = AppColors.Title,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppColors.Card,
    unfocusedContainerColor = AppColors.Card,
    disabledContainerColor = AppColors.Card,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = AppColors.Title,
    unfocusedTextColor = AppColors.Title,
    cursorColor = AppColors.Accent,
)
