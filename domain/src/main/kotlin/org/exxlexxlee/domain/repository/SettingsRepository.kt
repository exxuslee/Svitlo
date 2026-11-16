package org.exxlexxlee.domain.repository

import kotlinx.coroutines.flow.Flow
import org.exxlexxlee.domain.model.Country

interface SettingsRepository {
    fun country(): Country
    fun observeCountry(): Flow<Country>
    suspend fun setCountry(country: Country)
    fun isDarkTheme(): Boolean
    fun observeDarkTheme(): Flow<Boolean>
    suspend fun setDarkTheme(isDark: Boolean)
}
