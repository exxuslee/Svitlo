package org.exxlexxlee.data.remote.international

import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

interface InternationalOutageFeed {
    val sourceId: String

    // An overdue restoration estimate does not mean a current incident has ended.
    val retainsEndedOutages: Boolean

    suspend fun outages(source: OutageSource, address: OutageAddress?): List<InternationalOutage>
}
