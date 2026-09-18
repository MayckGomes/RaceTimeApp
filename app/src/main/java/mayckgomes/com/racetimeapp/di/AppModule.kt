package mayckgomes.com.racetimeapp.di

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.data.local.LocalCache
import mayckgomes.com.racetimeapp.data.remote.JolpicaApiClient
import mayckgomes.com.racetimeapp.data.repository.SeasonRepository
import mayckgomes.com.racetimeapp.ui.screen.TestViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        explicitNulls = false
                    }
                )
            }

            install(DefaultRequest) {
                header(HttpHeaders.UserAgent, "RaceTimeApp/2.0.0")
            }
        }
    }

    single { JolpicaApiClient(get()) }
    single { mayckgomes.com.racetimeapp.data.remote.JolpicaApiService(get()) }
}

val viewModelModule = module {
    viewModel { TestViewModel(get()) }
    viewModel { mayckgomes.com.racetimeapp.presentation.home.HomeViewModel(get(), get()) }
}

val appModule = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
val databaseModule = module {
    single { androidContext().getSharedPreferences("racetime_app_cache", Context.MODE_PRIVATE) }
    single { LocalCache(get()) }
}

val repositoryModule = module {
    single { SeasonRepository(get()) }
}

val viewModelModule = module {
    viewModel { TestViewModel(get()) }
}

val appModule = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
