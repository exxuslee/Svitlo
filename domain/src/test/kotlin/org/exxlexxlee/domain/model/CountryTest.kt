package org.exxlexxlee.domain.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryTest {
    @Test fun localeCountryResolvesSharedLanguages() {
        assertEquals(Country.CANADA, Country.forLocale(Locale.forLanguageTag("en-CA")))
        assertEquals(Country.SOUTH_AFRICA, Country.forLocale(Locale.forLanguageTag("en-ZA")))
        assertEquals(Country.IRELAND, Country.forLocale(Locale.forLanguageTag("en-IE")))
        assertEquals(Country.MEXICO, Country.forLocale(Locale.forLanguageTag("es-MX")))
        assertEquals(Country.UKRAINE, Country.forLocale(Locale.forLanguageTag("ru-UA")))
    }

    @Test fun languageIsUsedWhenCountryIsMissingOrUnsupported() {
        assertEquals(Country.POLAND, Country.forLocale(Locale.forLanguageTag("pl")))
        assertEquals(Country.PAKISTAN, Country.forLocale(Locale.forLanguageTag("ur")))
        assertEquals(Country.UNITED_STATES, Country.forLocale(Locale.forLanguageTag("en-US")))
        assertEquals(Country.CANADA, Country.forLocale(Locale.FRENCH))
        assertEquals(Country.ECUADOR, Country.forLocale(Locale.forLanguageTag("es")))
        assertEquals(Country.KENYA, Country.forLocale(Locale.forLanguageTag("sw")))
    }

    @Test fun unknownLanguageFallsBackToUkraine() {
        assertEquals(Country.UKRAINE, Country.forLocale(Locale.GERMANY))
        assertEquals(Country.UKRAINE, Country.forLocale(Locale.forLanguageTag("ru-RU")))
        assertEquals(Country.UKRAINE, Country.forLocale(Locale.ROOT))
    }

    @Test fun storedCodesAreStableAndUnknownValuesCanFallBackToLocale() {
        Country.entries.forEach { assertEquals(it, Country.fromCode(it.code.lowercase(Locale.ROOT))) }
        assertNull(Country.fromCode(null))
        assertNull(Country.fromCode("unknown"))
    }
}
