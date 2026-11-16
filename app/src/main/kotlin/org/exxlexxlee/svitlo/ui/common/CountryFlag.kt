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
    Country.FRANCE -> R.drawable.flag_fr
    Country.FINLAND -> R.drawable.flag_fi
    Country.AUSTRALIA -> R.drawable.flag_au
    Country.NEW_ZEALAND -> R.drawable.flag_nz
    Country.INDIA -> R.drawable.flag_in
    Country.PHILIPPINES -> R.drawable.flag_ph
    Country.CHILE -> R.drawable.flag_cl
    Country.BRAZIL -> R.drawable.flag_br
    Country.ISRAEL -> R.drawable.flag_il
}
