package org.exxlexxlee.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

class GithubJsonApi(
    private val client: HttpClient,
) {
    suspend fun fetch(url: String): String = client.get(url).bodyAsText()
}
