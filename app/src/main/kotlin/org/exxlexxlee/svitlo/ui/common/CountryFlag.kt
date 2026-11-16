package org.exxlexxlee.svitlo.ui.common

import androidx.annotation.DrawableRes
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.svitlo.R

@DrawableRes
fun Country.flagRes(): Int = when (this) {
    Country.UKRAINE -> R.drawable.flag_ua
    Country.SOUTH_AFRICA -> R.drawable.flag_za
    Country.UNITED_KINGDOM -> R.drawable.flag_gb
    Country.CANADA -> R.drawable.flag_ca
    Country.POLAND -> R.drawable.flag_pl
    Country.PAKISTAN -> R.drawable.flag_pk
    Country.ECUADOR -> R.drawable.flag_ec
    Country.IRELAND -> R.drawable.flag_ie
    Country.KENYA -> R.drawable.flag_ke
    Country.MEXICO -> R.drawable.flag_mx
    Country.UNITED_STATES -> R.drawable.flag_us
}
