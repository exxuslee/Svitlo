package org.exxlexxlee.data.remote.github

import io.ktor.client.HttpClient
import org.exxlexxlee.data.remote.http.ConditionalHttpClient

class GithubJsonApi(
    client: HttpClient,
    private val http: ConditionalHttpClient = ConditionalHttpClient(client),
) {
    suspend fun <T> fetch(url: String, decode: (String) -> T): T =
        http.get(url, source = "github_json", decode = decode)
}
