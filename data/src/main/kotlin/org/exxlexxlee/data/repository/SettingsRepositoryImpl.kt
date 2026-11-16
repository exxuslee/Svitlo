package org.exxlexxlee.data.repository

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.exxlexxlee.domain.repository.SettingsRepository
import org.exxlexxlee.domain.model.Country

class SettingsRepositoryImpl(
    context: Context,
) : SettingsRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val isSystemDark: Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

    private val initialDarkTheme: Boolean =
        if (prefs.contains(KEY_DARK_THEME)) {
            prefs.getBoolean(KEY_DARK_THEME, false)
        } else {
            isSystemDark
        }

    private val darkThemeFlow = MutableStateFlow(initialDarkTheme)

    // System resources keep the phone locale independent of the app language override.
    private val countryFlow = MutableStateFlow(
        Country.fromCode(prefs.getString(KEY_COUNTRY, null))
            ?: Country.forLocale(Resources.getSystem().configuration.locales[0]),
    )

    override fun country(): Country = countryFlow.value

    override fun observeCountry(): Flow<Country> = countryFlow.asStateFlow()

    override suspend fun setCountry(country: Country) {
        prefs.edit { putString(KEY_COUNTRY, country.code) }
        countryFlow.value = country
    }

    override fun isDarkTheme(): Boolean = darkThemeFlow.value

    override fun observeDarkTheme(): Flow<Boolean> = darkThemeFlow.asStateFlow()

    override suspend fun setDarkTheme(isDark: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_THEME, isDark) }
        darkThemeFlow.value = isDark
    }

    companion object {
        private const val PREFS_NAME = "svitlo_settings"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_COUNTRY = "country"
    }
}
