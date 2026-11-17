package org.exxlexxlee.svitlo.features.international.models

import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.svitlo.features.address.models.Event as DetailsEvent

sealed interface Event {
    data class CountrySelected(val country: Country) : Event
    data class SourceSelected(val source: OutageSource) : Event
    data class QueryChanged(val value: String) : Event
    data class LocationSelected(val location: String) : Event
    data class CityQueryChanged(val value: String) : Event
    data class StreetQueryChanged(val value: String) : Event
    data class HouseChanged(val value: String) : Event
    data class OptionSelected(val option: AddressOption) : Event
    data class DetailsChanged(val event: DetailsEvent) : Event
    data object Search : Event
    data object Refresh : Event
    data object Next : Event
    data object Back : Event
    data object Close : Event
    data object Save : Event
}
