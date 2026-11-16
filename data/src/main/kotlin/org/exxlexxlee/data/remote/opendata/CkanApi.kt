package org.exxlexxlee.data.remote.opendata

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.exxlexxlee.data.remote.http.ConditionalHttpClient

class CkanApi(private val http: ConditionalHttpClient, private val json: Json) {
    suspend fun records(
        portal: String,
        resourceId: String,
        filters: Map<String, String> = emptyMap(),
        sort: String = "_id asc",
        pageSize: Int = 100,
        maxRecords: Int = 10000,
    ): List<JsonObject> {
        require(resourceId.isNotBlank() && pageSize in 1..1000 && maxRecords > 0)
        val records = mutableListOf<JsonObject>()
        var offset = 0
        do {
            val url = portalUrl(portal, "api", "3", "action", "datastore_search").apply {
                parameters.append("resource_id", resourceId)
                parameters.append("limit", pageSize.toString())
                parameters.append("offset", offset.toString())
                parameters.append("sort", sort)
                parameters.append(
                    "filters",
                    JsonObject(filters.mapValues { JsonPrimitive(it.value) }).toString()
                )
            }.buildString()
            val page = http.get(url, "ckan:$resourceId") {
                val result = result(it)
                val total = result.getValue("total").jsonPrimitive.int
                val rows = result.getValue("records").jsonArray.map { row -> row.jsonObject }
                require(total in 0..maxRecords) { "CKAN record limit reached" }
                require(rows.isNotEmpty() || offset >= total) { "Incomplete CKAN response" }
                total to rows
            }
            records += page.second
            offset += page.second.size
            require(offset <= maxRecords) { "CKAN record limit reached" }
        } while (offset < page.first)
        return records
    }

    /** Resolve current resource IDs/URLs from package metadata instead of pinning stale download links. */
    suspend fun resources(portal: String, dataset: String): List<JsonObject> {
        val url = portalUrl(portal, "api", "3", "action", "package_show").apply {
            parameters.append("id", dataset)
        }.buildString()
        return http.get(url, "ckan:$dataset") { raw ->
            result(raw).getValue("resources").jsonArray.map { it.jsonObject }
        }
    }

    private fun result(raw: String): JsonObject {
        val root = json.parseToJsonElement(raw).jsonObject
        require(root.getValue("success").jsonPrimitive.boolean) { "CKAN action failed" }
        return root.getValue("result").jsonObject
    }
}
