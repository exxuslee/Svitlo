package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.svitlo.R
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState

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
            .background(if (isSelected) AddressPalette.AccentSoft else AddressPalette.Card)
            .then(
                if (isSelected) Modifier.border(
                    1.5.dp,
                    AddressPalette.Accent,
                    RoundedCornerShape(18.dp)
                )
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
