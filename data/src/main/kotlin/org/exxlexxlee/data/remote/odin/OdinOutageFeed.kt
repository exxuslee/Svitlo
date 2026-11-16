package org.exxlexxlee.data.remote.odin

import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class OdinOutageFeed(
    private val odin: OdinApi,
) : InternationalOutageFeed {
    override val sourceId = "odin"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) =
        odin.outages(address?.house?.takeIf { it.matches(Regex("[0-9]{5}")) })
}
