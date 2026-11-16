package org.exxlexxlee.domain.usecase

import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.repository.SettingsRepository

class GetCountry(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Country = settingsRepository.country()
}

class ObserveCountry(private val settingsRepository: SettingsRepository) {
    operator fun invoke() = settingsRepository.observeCountry()
}

class SetCountry(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(country: Country) = settingsRepository.setCountry(country)
}
