package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.exxlexxlee.domain.repository.SettingsRepository

class ObserveDarkTheme(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = settingsRepository.observeDarkTheme()
}

class GetDarkTheme(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Boolean = settingsRepository.isDarkTheme()
}

class SetDarkTheme(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(isDark: Boolean) = settingsRepository.setDarkTheme(isDark)
}
