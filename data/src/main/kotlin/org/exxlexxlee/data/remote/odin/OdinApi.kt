package org.exxlexxlee.data.remote.odin

import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.mapper.odin.OdinMapper
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.exxlexxlee.domain.model.InternationalOutage

/** Public /odi API; /odin endpoints require credentials and are intentionally not used. */
class OdinApi(private val http: ConditionalHttpClient, json: Json) {
    private val mapper = OdinMapper(json)

    suspend fun outages(countyFips: String? = null): List<InternationalOutage> {
        require(countyFips == null || countyFips.matches(Regex("[0-9]{5}")))
        val url = URLBuilder("https://odin.ornl.gov/odi/map").apply {
            parameters.append("scale", "COUNTY")
            countyFips?.let { parameters.append("countyCd", it) }
        }.buildString()
        return http.get(url, "odin") { raw ->
            mapper.parse(raw).filter { countyFips == null || it.id == "odin-$countyFips" }
        }
    }
}
