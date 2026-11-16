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
)

data class AddressOption(val id: Long, val name: String)

data class OutageAddress(val cityId: Long, val streetId: Long, val house: String)
