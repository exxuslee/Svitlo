package org.exxlexxlee.domain.model

import java.util.Locale

enum class Country(val code: String) {
    UKRAINE("UA"),
    SOUTH_AFRICA("ZA"),
    UNITED_KINGDOM("GB"),
    CANADA("CA"),
    POLAND("PL"),
    PAKISTAN("PK"),
    ECUADOR("EC"),
    IRELAND("IE"),
    KENYA("KE"),
    MEXICO("MX");

    companion object {
        fun fromCode(code: String?): Country? = entries.firstOrNull {
            it.code.equals(code, ignoreCase = true)
        }

        fun forLocale(locale: Locale): Country = fromCode(locale.country) ?: when (locale.language) {
            "uk" -> UKRAINE
            "af", "zu", "xh", "st", "tn", "ts", "ss", "ve", "nr", "nso" -> SOUTH_AFRICA
            "en", "cy" -> UNITED_KINGDOM
            "fr" -> CANADA
            "pl" -> POLAND
            "ur" -> PAKISTAN
            "es", "qu" -> ECUADOR
            "ga" -> IRELAND
            "sw" -> KENYA
            else -> UKRAINE
        }
    }
}
