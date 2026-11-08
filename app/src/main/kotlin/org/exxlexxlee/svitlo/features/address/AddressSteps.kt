package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.features.address.models.detailLabel

private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun RegionStep(viewState: ViewState, eventHandler: (Event) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(viewState.areas, key = { it.id }) { area ->
            AreaCard(
                area = area,
                isSelected = area.id == viewState.selectedArea?.id,
            ) { eventHandler(Event.AreaSelected(area)) }
        }
        errorItem(viewState.error)
    }
}

@Composable
fun SupplierStep(viewState: ViewState, eventHandler: (Event) -> Unit) {
    val suppliers = viewState.selectedArea?.suppliers.orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(suppliers, key = { it.region.id }) { supplier ->
            SupplierCard(
                supplier = supplier,
                isSelected = supplier.region.id == viewState.selectedSupplier?.region?.id,
            ) { eventHandler(Event.SupplierSelected(supplier)) }
        }
        errorItem(viewState.error)
    }
}

@Composable
fun GroupStep(viewState: ViewState, eventHandler: (Event) -> Unit) {
    if (viewState.isLoadingGroups) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AddressPalette.Accent)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(3) }) {
            Text(
                text = stringResource(R.string.find_group_by_address),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AddressPalette.Banner)
                    .clickable { eventHandler(Event.FindByAddress) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                color = AddressPalette.Title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
        items(viewState.sortedGroups, key = { it.id }) { group ->
            GroupChip(
                group = group,
                isSelected = group.id == viewState.selectedGroup?.id,
            ) { eventHandler(Event.GroupSelected(group)) }
        }
        if (viewState.error != null) {
            item(span = { GridItemSpan(3) }) {
                Text(text = viewState.error, color = AddressPalette.Title)
            }
        }
    }
}

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
                    color = AddressPalette.Subtitle,
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
                    .background(Color.White)
                    .clickable { eventHandler(Event.OpenReminder) }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                color = AddressPalette.Title,
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
                        checkedColor = AddressPalette.Accent,
                        uncheckedColor = AddressPalette.Subtitle,
                    ),
                )
                Text(
                    text = stringResource(R.string.quiet_night),
                    color = AddressPalette.Title,
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
                    color = AddressPalette.Title,
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
                containerColor = AddressPalette.Save,
                contentColor = AddressPalette.Title,
                disabledContainerColor = AddressPalette.Banner,
                disabledContentColor = AddressPalette.Subtitle,
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
private fun AreaCard(
    area: ServiceArea,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, CardShape)
            .clip(CardShape)
            .background(if (isSelected) AddressPalette.AccentSoft else Color.White)
            .then(
                if (isSelected) Modifier.border(1.5.dp, AddressPalette.Accent, CardShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = area.name,
            color = AddressPalette.Title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = area.suppliers.joinToString(", ") { it.region.name },
            modifier = Modifier.padding(top = 6.dp),
            color = AddressPalette.Body,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: Supplier,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, CardShape)
            .clip(CardShape)
            .background(if (isSelected) AddressPalette.AccentSoft else Color.White)
            .then(
                if (isSelected) Modifier.border(1.5.dp, AddressPalette.Accent, CardShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = supplier.region.name,
            modifier = Modifier.weight(1f),
            color = AddressPalette.Title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
        )
        if (supplier.primary) {
            Text(
                text = stringResource(R.string.supplier_primary),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AddressPalette.Accent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

@Composable
private fun GroupChip(
    group: OutageGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) AddressPalette.AccentSoft else Color.White)
            .then(
                if (isSelected) Modifier.border(1.5.dp, AddressPalette.Accent, RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = group.id,
            color = AddressPalette.Title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 8.dp),
        color = AddressPalette.Subtitle,
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
                            .background(if (isSelected) AddressPalette.AccentSoft else Color.White)
                            .border(1.5.dp, AddressPalette.Accent, RoundedCornerShape(16.dp))
                            .clickable { onSelect(icon) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon.image,
                            contentDescription = icon.id,
                            tint = AddressPalette.Title,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = AddressPalette.Title,
    unfocusedTextColor = AddressPalette.Title,
    cursorColor = AddressPalette.Accent,
)

private fun androidx.compose.foundation.lazy.LazyListScope.errorItem(error: String?) {
    if (error != null) {
        item {
            Text(text = error, color = AddressPalette.Title)
        }
    }
}
