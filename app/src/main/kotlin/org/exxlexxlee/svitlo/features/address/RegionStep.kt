package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.ui.theme.AppColors


private val CardShape = RoundedCornerShape(20.dp)

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
            .background(if (isSelected) AppColors.AccentSoft else AppColors.Card)
            .then(
                if (isSelected) Modifier.border(1.5.dp, AppColors.Accent, CardShape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = area.name,
            color = AppColors.Title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = area.suppliers.joinToString(", ") { it.region.name },
            modifier = Modifier.padding(top = 6.dp),
            color = AppColors.Body,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
    }
}