package org.exxlexxlee.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun isDarkTheme(): Boolean
    fun observeDarkTheme(): Flow<Boolean>
    suspend fun setDarkTheme(isDark: Boolean)
}
