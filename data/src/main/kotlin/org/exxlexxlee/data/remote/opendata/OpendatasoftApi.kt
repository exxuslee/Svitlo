package org.exxlexxlee.data.remote.opendata

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.remote.http.ConditionalHttpClient

/** Explore v2.1 connector shared by utility portals. ODSQL is passed as encoded query parameters. */
class OpendatasoftApi(private val http: ConditionalHttpClient, private val json: Json) {
    suspend fun records(
        portal: String,
        dataset: String,
        select: String? = null,
        where: String? = null,
        orderBy: String,
        pageSize: Int = 100,
        apiKey: String? = null,
        validate: (List<JsonObject>) -> Unit = {},
    ): List<JsonObject> {
        require(dataset.isNotBlank() && pageSize in 1..100)
        val records = mutableListOf<JsonObject>()
        var offset = 0
        do {
            val url = portalUrl(
                portal,
                "api",
                "explore",
                "v2.1",
                "catalog",
                "datasets",
                dataset,
                "records"
            ).apply {
                parameters.append("limit", pageSize.toString())
                parameters.append("offset", offset.toString())
                parameters.append("order_by", orderBy)
                select?.let { parameters.append("select", it) }
                where?.let { parameters.append("where", it) }
            }.buildString()
            val page = http.get(
                url,
                "ods:$dataset",
                apiKey?.let { mapOf("Authorization" to "Apikey $it") }.orEmpty()
            ) {
                val root = json.parseToJsonElement(it).jsonObject
                val total = root.getValue("total_count").jsonPrimitive.int
                val rows = root.getValue("results").jsonArray.map { row -> row.jsonObject }
                require(total >= 0 && (rows.isNotEmpty() || offset >= total)) { "Incomplete Opendatasoft response" }
                validate(rows)
                total to rows
            }
            records += page.second
            offset += page.second.size
            require(offset < 10000 || offset >= page.first) { "Opendatasoft pagination limit reached" }
        } while (offset < page.first)
        return records
    }
}

internal fun portalUrl(portal: String, vararg segments: String): URLBuilder =
    URLBuilder(portal).apply {
        require(protocol == URLProtocol.HTTPS && host.isNotBlank() && parameters.isEmpty() && fragment.isEmpty())
        appendPathSegments(*segments)
    }
