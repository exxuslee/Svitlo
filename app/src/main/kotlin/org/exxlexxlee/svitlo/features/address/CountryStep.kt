package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.svitlo.features.address.models.Event
import org.exxlexxlee.svitlo.features.address.models.ViewState
import org.exxlexxlee.svitlo.features.settings.countryNameRes
import org.exxlexxlee.svitlo.ui.common.flagRes

@Composable
fun CountryStep(state: ViewState, eventHandler: (Event) -> Unit) {
    val countries = listOf(state.country) + Country.entries.filter { it != state.country }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(countries, key = { it.code }) { country ->
            OutlinedButton(
                onClick = { eventHandler(Event.CountrySelected(country)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(country.flagRes()),
                    contentDescription = null,
                    modifier = Modifier.size(width = 32.dp, height = 24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(country.countryNameRes()))
            }
        }
    }
}
