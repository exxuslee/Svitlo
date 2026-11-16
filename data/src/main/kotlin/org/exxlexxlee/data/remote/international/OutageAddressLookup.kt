package org.exxlexxlee.data.remote.international

import org.exxlexxlee.domain.model.AddressOption

interface OutageAddressLookup {
    suspend fun cities(query: String): List<AddressOption>
    suspend fun streets(cityId: Long, query: String): List<AddressOption>
}
