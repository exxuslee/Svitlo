package org.exxlexxlee.svitlo.features.address.models

import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Supplier
import org.exxlexxlee.svitlo.features.address.AddressIcon

enum class AddressStep(val index: Int) {
    Region(1),
    Supplier(2),
    Group(3),
    Details(4),
    ;

    val fraction: Float get() = index / 4f
}

data class ViewState(
    val areas: List<ServiceArea> = emptyList(),
    val step: AddressStep = AddressStep.Region,
    val selectedArea: ServiceArea? = null,
    val selectedSupplier: Supplier? = null,
    val groups: List<OutageGroup> = emptyList(),
    val selectedGroup: OutageGroup? = null,
    val addressName: String = "",
    val nameEdited: Boolean = false,
    val reminderMinutes: Int = 15,
    val quietNight: Boolean = false,
    val icon: AddressIcon = AddressIcon.Home,
    val isLoadingAreas: Boolean = true,
    val isLoadingGroups: Boolean = false,
    val showReminderPicker: Boolean = false,
    val showAddressSearchHint: Boolean = false,
    val error: String? = null,
) {
    val sortedGroups: List<OutageGroup>
        get() = groups.sortedWith { left, right -> compareNatural(left.id, right.id) }

    val canSave: Boolean
        get() = selectedSupplier != null && selectedGroup != null && addressName.isNotBlank()
}

fun OutageGroup.detailLabel(): String {
    val cleaned = name
        .removePrefix("Черга ")
        .removePrefix("Група ")
        .removePrefix("GPV")
        .trim()
    return cleaned.ifEmpty { id.removePrefix("GPV") }
}

private fun compareNatural(left: String, right: String): Int {
    val pattern = Regex("\\d+|\\D+")
    val leftParts = pattern.findAll(left).map { it.value }.toList()
    val rightParts = pattern.findAll(right).map { it.value }.toList()
    val count = minOf(leftParts.size, rightParts.size)
    for (index in 0 until count) {
        val leftNumber = leftParts[index].toIntOrNull()
        val rightNumber = rightParts[index].toIntOrNull()
        val compared = if (leftNumber != null && rightNumber != null) {
            leftNumber.compareTo(rightNumber)
        } else {
            leftParts[index].compareTo(rightParts[index])
        }
        if (compared != 0) return compared
    }
    return leftParts.size.compareTo(rightParts.size)
}
