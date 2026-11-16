package org.exxlexxlee.domain.repository

import org.exxlexxlee.domain.model.*

interface InternationalOutageRepository {
    fun sources(country: Country): List<OutageSource>
    suspend fun outages(source: OutageSource, address: OutageAddress? = null): List<InternationalOutage>
    suspend fun cities(query: String): List<AddressOption>
    suspend fun streets(cityId: Long, query: String): List<AddressOption>
}
