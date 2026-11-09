package org.exxlexxlee.data.di

import androidx.room.Room
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.exxlexxlee.data.catalog.RegionCatalog
import org.exxlexxlee.data.local.AppDatabase
import org.exxlexxlee.data.mapper.GpvMapper
import org.exxlexxlee.data.mapper.YasnoMapper
import org.exxlexxlee.data.remote.GithubJsonApi
import org.exxlexxlee.data.remote.ScheduleRemoteDataSource
import org.exxlexxlee.data.remote.YasnoApi
import org.exxlexxlee.data.repository.LocationRepositoryImpl
import org.exxlexxlee.data.repository.ScheduleRepositoryImpl
import org.exxlexxlee.data.repository.SelectionRepositoryImpl
import org.exxlexxlee.data.repository.SettingsRepositoryImpl
import org.exxlexxlee.domain.repository.LocationRepository
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.domain.repository.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
    single {
        val json: Json = get()
        HttpClient(OkHttp) {
            expectSuccess = true
            install(UserAgent) {
                agent = "Svitlo/1.0 (Android)"
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay()
            }
        }
    }
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "svitlo.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().placeDao() }
    single { get<AppDatabase>().scheduleDao() }
    single { RegionCatalog(androidContext(), get()) }
    single { YasnoApi(get()) }
    single { GithubJsonApi(get()) }
    single { YasnoMapper(get()) }
    single { GpvMapper(get()) }
    single { ScheduleRemoteDataSource(get(), get(), get(), get()) }
    single<LocationRepository> {
        LocationRepositoryImpl(get(), get())
    }
    single<ScheduleRepository> {
        ScheduleRepositoryImpl(get(), get(), get())
    }
    single<SelectionRepository> { SelectionRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }
}
