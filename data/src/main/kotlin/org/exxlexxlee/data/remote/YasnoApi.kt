package org.exxlexxlee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class YasnoApi(
    private val client: HttpClient,
) {
    suspend fun fetchPlannedOutages(regionId: Int, dsoId: Int): String {
        return client.get(
            "https://app.yasno.ua/api/blackout-service/public/shutdowns/regions/$regionId/dsos/$dsoId/planned-outages",
        ).bodyAsText()
    }
}
