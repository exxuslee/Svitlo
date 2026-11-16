package org.exxlexxlee.data.remote.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ConditionalHttpClientTest {
    @Test fun revalidatesWithEtagAndLastModifiedAndDecodesCachedBodyAgain() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { request ->
            requests++
            if (requests == 1) respond("42", headers = headersOf(
                HttpHeaders.ETag to listOf("\"v1\""), HttpHeaders.LastModified to listOf("Mon, 16 Nov 2026 10:00:00 GMT"),
            )) else {
                assertEquals("\"v1\"", request.headers[HttpHeaders.IfNoneMatch])
                assertEquals("Mon, 16 Nov 2026 10:00:00 GMT", request.headers[HttpHeaders.IfModifiedSince])
                respond("", HttpStatusCode.NotModified)
            }
        }) { expectSuccess = true }
        try {
            val http = ConditionalHttpClient(client)
            assertEquals(42, http.get("https://example.com/a", "test") { it.toInt() })
            // Simulates mapping an unchanged body with a new date/selection.
            assertEquals(43, http.get("https://example.com/a", "test") { it.toInt() + 1 })
        } finally { client.close() }
    }

    @Test fun schemaFailureDoesNotPoisonCachedBodyOrValidator() = runTest {
        var requests = 0
        val failures = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when (++requests) {
                1 -> respond("42", headers = headersOf(HttpHeaders.ETag, "v1"))
                2 -> respond("<html>Unavailable</html>", headers = headersOf(HttpHeaders.ETag, "v2"))
                else -> {
                    assertEquals("v1", request.headers[HttpHeaders.IfNoneMatch])
                    respond("", HttpStatusCode.NotModified)
                }
            }
        })
        try {
            val http = ConditionalHttpClient(client, FeedFailureReporter { source, _ -> failures += source })
            http.get("https://example.com/a", "test") { it.toInt() }
            try { http.get("https://example.com/a", "test") { it.toInt() }; fail() } catch (_: NumberFormatException) { }
            assertEquals(42, http.get("https://example.com/a", "test") { it.toInt() })
            assertEquals(listOf("test"), failures)
        } finally { client.close() }
    }

    @Test fun rejectsUnsolicited304AndHttpErrorsEvenWithoutExpectSuccess() = runTest {
        for (status in listOf(HttpStatusCode.NotModified, HttpStatusCode.Forbidden, HttpStatusCode.ServiceUnavailable)) {
            val client = HttpClient(MockEngine { respond("", status) })
            try {
                try { ConditionalHttpClient(client).get("https://example.com/a", "test") { it }; fail() }
                catch (_: IllegalStateException) { }
            } finally { client.close() }
        }
    }

    @Test fun evictionAndDifferentCredentialsNeverReuseAnotherBody() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertNull(request.headers[HttpHeaders.IfNoneMatch])
            respond("42", headers = headersOf(HttpHeaders.ETag, "v1"))
        })
        try {
            val http = ConditionalHttpClient(client, capacity = 1)
            http.get("https://example.com/a", "test", mapOf("x-api-key" to "first")) { it }
            http.get("https://example.com/a", "test", mapOf("x-api-key" to "second")) { it }
            http.get("https://example.com/a", "test", mapOf("x-api-key" to "first")) { it }
        } finally { client.close() }
    }

    @Test fun cancellationPropagatesWithoutReportingProviderFailure() = runTest {
        val failures = mutableListOf<Exception>()
        val client = HttpClient(MockEngine { throw CancellationException("cancelled") })
        try {
            val http = ConditionalHttpClient(client, FeedFailureReporter { _, failure -> failures += failure })
            try { http.get("https://example.com/a", "test") { it }; fail() } catch (_: CancellationException) { }
            assertTrue(failures.isEmpty())
        } finally { client.close() }
    }
}
