package org.exxlexxlee.svitlo.features.address.models

import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.svitlo.features.address.AddressIcon

sealed interface Event {
    data object Back : Event
    data object Close : Event
    data class AreaSelected(val area: ServiceArea) : Event
    data class SupplierSelected(val supplier: Supplier) : Event
    data class GroupSelected(val group: OutageGroup) : Event
    data object FindByAddress : Event
    data object DismissAddressSearch : Event
    data class AddressNameChanged(val name: String) : Event
    data object OpenReminder : Event
    data class ReminderSelected(val minutes: Int) : Event
    data object DismissReminder : Event
    data class QuietNightChanged(val enabled: Boolean) : Event
    data class IconSelected(val icon: AddressIcon) : Event
    data object Save : Event
}
