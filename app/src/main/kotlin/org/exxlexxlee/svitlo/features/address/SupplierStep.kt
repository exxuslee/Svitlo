package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState


private val CardShape = RoundedCornerShape(20.dp)

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