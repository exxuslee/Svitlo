package org.exxlexxlee.data

import io.ktor.client.HttpClient
import java.time.Clock
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.di.internationalOutageModule
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.exxlexxlee.data.remote.http.FeedFailureReporter
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.koin.dsl.koinApplication
import org.koin.dsl.module

internal fun internationalOutageRepository(
    client: HttpClient,
    json: Json,
    clock: Clock,
    reporter: FeedFailureReporter = FeedFailureReporter.NONE,
): InternationalOutageRepository {
    val application = koinApplication {
        modules(module {
            single { client }
            single { json }
            single { ConditionalHttpClient(client) }
            single { reporter }
        }, internationalOutageModule(clock))
    }
    return try {
        application.koin.get<InternationalOutageRepository>()
    } finally {
        application.close()
    }
}
