package org.exxlexxlee.data.remote.yasno

import io.ktor.client.HttpClient
import org.exxlexxlee.data.remote.http.ConditionalHttpClient

class YasnoApi(
    client: HttpClient,
    private val http: ConditionalHttpClient = ConditionalHttpClient(client),
) {
    suspend fun <T> fetchPlannedOutages(regionId: Int, dsoId: Int, decode: (String) -> T): T {
        require(regionId > 0 && dsoId > 0)
        return http.get(
            "https://app.yasno.ua/api/blackout-service/public/shutdowns/regions/$regionId/dsos/$dsoId/planned-outages",
            source = "yasno:$regionId:$dsoId",
            decode = decode,
        )
    }
}
