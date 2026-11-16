package org.exxlexxlee.data.remote.manitoba

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.exxlexxlee.data.mapper.manitoba.ManitobaMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class ManitobaOutageFeed(
    private val client: HttpClient,
    private val manitoba: ManitobaMapper,
) : InternationalOutageFeed {
    override val sourceId = "manitoba"
    override val retainsEndedOutages = false

    override suspend fun outages(source: OutageSource, address: OutageAddress?) =
        manitoba.parse(client.get("https://services2.arcgis.com/QoeQkfdOG126FqSi/ArcGIS/rest/services/Manitoba_Hydro_Planned_Power_Outages/FeatureServer/0/query") {
            parameter("where", "1=1")
            parameter("outFields", "*")
            parameter("returnGeometry", false)
            parameter("f", "json")
        }.bodyAsText())
}
