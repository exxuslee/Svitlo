package org.exxlexxlee.data.remote.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Reports transport and schema failures without recording payloads or address queries. */
fun interface FeedFailureReporter {
    fun report(source: String, failure: Exception)

    companion object {
        val NONE = FeedFailureReporter { _, _ -> }
    }
}

/** Keeps only validated bodies. A 304 reuses the body, but callers still apply their current clock. */
class ConditionalHttpClient(
    private val client: HttpClient,
    private val reporter: FeedFailureReporter = FeedFailureReporter.NONE,
    private val capacity: Int = 64,
) {
    private data class Entry(val body: String, val etag: String?, val modified: String?)
    private data class Key(val url: String, val headers: Map<String, String>)
    private class Gate(val mutex: Mutex = Mutex(), var users: Int = 0)

    private val entries = linkedMapOf<Key, Entry>()
    private val gates = mutableMapOf<Key, Gate>()
    private val mutex = Mutex()

    init {
        require(capacity > 0)
    }

    suspend fun <T> get(
        url: String,
        source: String,
        headers: Map<String, String> = emptyMap(),
        decode: (String) -> T,
    ): T = try {
        val key = Key(url, headers.toMap())
        val gate = mutex.withLock { gates.getOrPut(key) { Gate() }.also { it.users++ } }
        try {
            gate.mutex.withLock {
                val previous = mutex.withLock { entries[key] }
                val response = client.get(url) {
                    expectSuccess = false
                    headers.forEach { (name, value) -> header(name, value) }
                    previous?.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                    previous?.modified?.let { header(HttpHeaders.IfModifiedSince, it) }
                }
                if (response.status == HttpStatusCode.NotModified) {
                    checkNotNull(previous) { "Unsolicited 304 from $source" }
                    decode(previous.body)
                } else {
                    check(response.status == HttpStatusCode.OK) { "$source HTTP ${response.status.value}" }
                    val body = response.bodyAsText()
                    val result = decode(body)
                    // Never replace a known-good entry with an error page or a changed schema.
                    mutex.withLock {
                        entries.remove(key)
                        entries[key] = Entry(
                            body,
                            response.headers[HttpHeaders.ETag],
                            response.headers[HttpHeaders.LastModified]
                        )
                        while (entries.size > capacity) entries.remove(entries.keys.first())
                    }
                    result
                }
            }
        } finally {
            // Cancellation must release the gate reference as well as the network request.
            withContext(NonCancellable) {
                mutex.withLock { if (--gate.users == 0) gates.remove(key) }
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        reporter.report(source, failure)
        throw failure
    }
}
