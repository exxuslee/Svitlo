package org.exxlexxlee.svitlo.features.address

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import org.exxlexxlee.svitlo.ui.theme.AppColors

fun LazyListScope.errorItem(error: String?) {
    if (error != null) {
        item {
            Text(text = error, color = AppColors.Title)
        }
    }
}