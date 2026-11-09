package org.exxlexxlee.data.repository

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.exxlexxlee.domain.repository.SettingsRepository

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

    override fun isDarkTheme(): Boolean = darkThemeFlow.value

    override fun observeDarkTheme(): Flow<Boolean> = darkThemeFlow.asStateFlow()

    override suspend fun setDarkTheme(isDark: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_THEME, isDark) }
        darkThemeFlow.value = isDark
    }

    companion object {
        private const val PREFS_NAME = "svitlo_settings"
        private const val KEY_DARK_THEME = "dark_theme"
    }
}
