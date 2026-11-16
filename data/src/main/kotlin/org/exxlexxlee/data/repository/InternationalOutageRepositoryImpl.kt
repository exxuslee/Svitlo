package org.exxlexxlee.data.repository

import java.time.Clock
import kotlinx.coroutines.CancellationException
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.remote.http.FeedFailureReporter
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.data.remote.international.OutageAddressLookup
import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.domain.repository.InternationalOutageRepository

class InternationalOutageRepositoryImpl(
    feeds: List<InternationalOutageFeed>,
    private val addressLookup: OutageAddressLookup,
    private val clock: Clock = Clock.systemUTC(),
    private val reporter: FeedFailureReporter = FeedFailureReporter.NONE,
) : InternationalOutageRepository {
    private val feedsById = feeds.associateBy { it.sourceId }.also {
        require(it.size == feeds.size) { "Duplicate outage feed identifiers" }
    }

    override fun sources(country: Country) =
        InternationalSources.all.filter { it.country == country }

    override suspend fun outages(
        source: OutageSource,
        address: OutageAddress?
    ): List<InternationalOutage> = try {
        require(source in sources(source.country))
        require(
            source.access in listOf(
                SourceAccess.LIVE,
                SourceAccess.ADDRESS
            )
        ) { "Source has no available feed" }
        val feed = feedsById[source.id] ?: error("Source does not provide a verified live feed")
        feed.outages(source, address)
            .filter { feed.retainsEndedOutages || it.end?.let { end -> end > clock.instant() } != false }
            .distinctBy { it.id }
            .sortedBy { it.start }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        reporter.report(source.id, failure)
        throw failure
    }

    override suspend fun cities(query: String): List<AddressOption> = addressLookup.cities(query)

    override suspend fun streets(cityId: Long, query: String): List<AddressOption> =
        addressLookup.streets(cityId, query)
}
