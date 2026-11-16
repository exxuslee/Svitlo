package org.exxlexxlee.domain.model

data class AddressDetails(
    val name: String,
    val reminderMinutes: Int = 15,
    val quietNight: Boolean = false,
    val iconId: String = "home",
    val country: Country = Country.UKRAINE,
    val sourceId: String? = null,
    val filter: String = "",
    val cityId: Long? = null,
    val streetId: Long? = null,
    val house: String = "",
)

data class Place(
    val id: Long,
    val selection: Selection,
)

sealed interface Selection {
    val region: Region
    val group: OutageGroup
    val details: AddressDetails?

    data class ByGroup(
        override val region: Region,
        override val group: OutageGroup,
        override val details: AddressDetails? = null,
    ) : Selection

    data class ByAddress(
        val address: Address,
        override val group: OutageGroup,
        override val details: AddressDetails? = null,
    ) : Selection {
        override val region: Region get() = address.region
    }
}
