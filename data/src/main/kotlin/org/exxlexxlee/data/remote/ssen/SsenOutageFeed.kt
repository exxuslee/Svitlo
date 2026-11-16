package org.exxlexxlee.data.remote.ssen

import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class SsenOutageFeed(
    private val ssen: SsenApi,
) : InternationalOutageFeed {
    override val sourceId = "ssen"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) = ssen.outages()
}
