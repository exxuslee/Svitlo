package org.exxlexxlee.domain.model

import java.time.Instant

enum class SourceAccess { LIVE, ADDRESS, WEBSITE, ARCHIVED, PERMISSION_REQUIRED, UNAVAILABLE }

data class OutageSource(
    val id: String,
    val country: Country,
    val name: String,
    val website: String,
    val zoneId: String,
    val access: SourceAccess,
)

data class InternationalOutage(
    val id: String,
    val location: String,
    val description: String,
    val start: Instant?,
    val end: Instant?,
    val areaOnly: Boolean = true,
    val kind: OutageKind = OutageKind.ACTUAL,
    val endIsEstimate: Boolean = false,
    val affectedCustomers: Int? = null,
    val isAreaSummary: Boolean = false,
    val percentWithoutPower: Double? = null,
    val point: GeoPoint? = null,
)

data class AddressOption(val id: Long, val name: String)

data class OutageAddress(val cityId: Long, val streetId: Long, val house: String)
