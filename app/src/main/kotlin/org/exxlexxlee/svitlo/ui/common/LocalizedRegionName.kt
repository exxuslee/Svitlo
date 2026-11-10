package org.exxlexxlee.svitlo.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.exxlexxlee.svitlo.R

// Source names and persisted address names remain unchanged; translate only the region picker.
@Composable
internal fun localizedRegionName(name: String): String =
    regionNames[name]?.let { stringResource(it) } ?: name

private val regionNames = mapOf(
    "Київ" to R.string.region_kyiv,
    "Київська область" to R.string.region_kyiv_region,
    "Вінницька область" to R.string.region_vinnytsia,
    "Дніпропетровська область" to R.string.region_dnipro,
    "Житомирська область" to R.string.region_zhytomyr,
    "Закарпатська область" to R.string.region_zakarpattia,
    "Запорізька область" to R.string.region_zaporizhzhia,
    "Івано-Франківська область" to R.string.region_ivano_frankivsk,
    "Львівська область" to R.string.region_lviv,
    "Одеська область" to R.string.region_odesa,
    "Полтавська область" to R.string.region_poltava,
    "Рівненська область" to R.string.region_rivne,
    "Тернопільська область" to R.string.region_ternopil,
    "Хмельницька область" to R.string.region_khmelnytskyi,
    "Чернігівська область" to R.string.region_chernihiv,
)
