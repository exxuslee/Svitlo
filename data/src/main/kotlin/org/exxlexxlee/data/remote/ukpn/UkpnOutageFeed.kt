package org.exxlexxlee.data.remote.ukpn

import org.exxlexxlee.data.mapper.ukpn.UkpnMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.data.remote.opendata.OpendatasoftApi
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class UkpnOutageFeed(
    private val ods: OpendatasoftApi,
    private val ukpn: UkpnMapper,
) : InternationalOutageFeed {
    override val sourceId = "ukpn"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) = ukpn.records(
        ods.records(
            portal = "https://ukpowernetworks.opendatasoft.com", dataset = "ukpn-live-faults",
            orderBy = "incidentreference", validate = { ukpn.records(it) },
        )
    )
}
