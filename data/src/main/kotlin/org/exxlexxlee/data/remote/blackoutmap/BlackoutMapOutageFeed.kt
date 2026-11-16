package org.exxlexxlee.data.remote.blackoutmap

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.exxlexxlee.data.mapper.blackoutmap.BlackoutMapMapper
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource

internal class BlackoutMapOutageFeed(
    private val client: HttpClient,
    private val blackoutMap: BlackoutMapMapper,
) : InternationalOutageFeed {
    override val sourceId = "blackoutmap"
    override val retainsEndedOutages = true

    override suspend fun outages(source: OutageSource, address: OutageAddress?) = run {
        val raw = client.get("https://blackoutmap.net/data/status.json").bodyAsText()
        // Prefer ISO2 country code when available; Ukraine is the primary scheduled feed today
        val code = if (source.country == Country.UKRAINE) "UA" else null
        blackoutMap.parse(raw, code)
    }
}
