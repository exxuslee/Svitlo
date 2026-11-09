package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text

fun LazyListScope.errorItem(error: String?) {
    if (error != null) {
        item {
            Text(text = error, color = AddressPalette.Title)
        }
    }
}