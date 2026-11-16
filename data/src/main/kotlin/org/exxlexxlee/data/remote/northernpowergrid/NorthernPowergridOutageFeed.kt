package org.exxlexxlee.data.remote.northernpowergrid

import org.exxlexxlee.data.mapper.northernpowergrid.NorthernPowergridMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.data.remote.opendata.OpendatasoftApi
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class NorthernPowergridOutageFeed(
    private val ods: OpendatasoftApi,
) : InternationalOutageFeed {
    override val sourceId = "northern_powergrid"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) =
        NorthernPowergridMapper.map(
            ods.records(
                portal = "https://northernpowergrid.opendatasoft.com",
                dataset = "live-power-cuts-data",
                where = "isaffected=1",
                orderBy = "reference,id",
                validate = { NorthernPowergridMapper.map(it) },
            )
        )
}
