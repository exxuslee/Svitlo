package org.exxlexxlee.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.exxlexxlee.data.catalog.InternationalSources
import org.exxlexxlee.data.remote.http.FeedFailureReporter
import org.exxlexxlee.data.remote.international.InternationalOutageFeed
import org.exxlexxlee.data.remote.international.OutageAddressLookup
import org.exxlexxlee.data.repository.InternationalOutageRepositoryImpl
import org.exxlexxlee.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class InternationalOutageStrategyTest {
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val source = InternationalSources.all.first { it.id == "ukpn" }
    private val addressLookup = object : OutageAddressLookup {
        override suspend fun cities(query: String): List<AddressOption> = error("No address lookup expected")
        override suspend fun streets(cityId: Long, query: String): List<AddressOption> = error("No address lookup expected")
    }

    @Test fun injectedFeedReceivesContextAndControlsExpiryWithoutSourceIdExceptions() = runTest {
        val address = OutageAddress(1, 2, "3")
        val records = listOf(
            event("later", start = now.plusSeconds(60)),
            event("expired", end = now.minusSeconds(1)),
            event("ending-now", end = now),
            event("unknown"),
            event("later", start = now.plusSeconds(120)),
            event("earlier", start = now),
        )
        // The same source can be backed by either policy; the repository must use the strategy.
        for (retain in listOf(false, true)) {
            val feed = feed(retain) { actualSource, actualAddress ->
                assertEquals(source, actualSource)
                assertEquals(address, actualAddress)
                records
            }
            val repository = InternationalOutageRepositoryImpl(listOf(feed), addressLookup, clock)
            val actual = repository.outages(source, address)
            val expected = if (retain) listOf("expired", "ending-now", "unknown", "earlier", "later")
                else listOf("unknown", "earlier", "later")
            assertEquals(expected, actual.map { it.id })
            assertEquals(now.plusSeconds(60), actual.last().start)
        }
    }

    @Test fun missingFeedIsReportedAsFailure() = runTest {
        val failures = mutableListOf<Pair<String, Throwable>>()
        val repository = InternationalOutageRepositoryImpl(
            emptyList(), addressLookup, clock,
            FeedFailureReporter { id, failure -> failures += id to failure },
        )
        try {
            repository.outages(source)
            fail("Expected a missing-feed failure")
        } catch (failure: IllegalStateException) {
            assertEquals("Source does not provide a verified live feed", failure.message)
            assertEquals(source.id, failures.single().first)
            assertSame(failure, failures.single().second)
        }
    }

    @Test fun cancellationPropagatesWithoutFailureReport() = runTest {
        val cancellation = CancellationException("Cancelled request")
        val repository = InternationalOutageRepositoryImpl(
            listOf(feed(false) { _, _ -> throw cancellation }), addressLookup, clock,
            FeedFailureReporter { _, _ -> fail("Cancellation must not be reported as a feed failure") },
        )
        try {
            repository.outages(source)
            fail("Expected cancellation")
        } catch (failure: CancellationException) {
            assertSame(cancellation, failure)
        }
    }

    private fun feed(
        retain: Boolean,
        load: suspend (OutageSource, OutageAddress?) -> List<InternationalOutage>,
    ) = object : InternationalOutageFeed {
        override val sourceId = source.id
        override val retainsEndedOutages = retain
        override suspend fun outages(source: OutageSource, address: OutageAddress?) = load(source, address)
    }

    private fun event(id: String, start: Instant? = null, end: Instant? = null) =
        InternationalOutage(id, "Area", "Outage", start, end)
}
