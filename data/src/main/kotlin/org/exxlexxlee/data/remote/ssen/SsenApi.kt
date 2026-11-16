package org.exxlexxlee.data.remote.ssen

import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.mapper.ssen.SsenMapper
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.exxlexxlee.domain.model.InternationalOutage

class SsenApi(private val http: ConditionalHttpClient, json: Json) {
    private val mapper = SsenMapper(json)

    suspend fun outages(): List<InternationalOutage> =
        http.get("$BASE/getallfaults", "ssen", decode = mapper::parse)

    suspend fun outage(reference: String): InternationalOutage {
        require(reference.isNotBlank())
        return http.get("$BASE/getbyid/${reference.encodeURLPathPart()}", "ssen", decode = mapper::parseFault)
    }

    private companion object {
        const val BASE = "https://external.distribution.prd.ssen.co.uk/opendataportal-prd/v4/api"
    }
}
