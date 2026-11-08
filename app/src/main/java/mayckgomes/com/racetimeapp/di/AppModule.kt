package mayckgomes.com.racetimeapp.di

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import java.time.Clock
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.data.local.PreferenceResponseCache
import mayckgomes.com.racetimeapp.data.local.ResponseCache
import mayckgomes.com.racetimeapp.data.remote.JolpicaGateway
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        val app = androidContext()
        val version = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        explicitNulls = false
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
            install(DefaultRequest) { header(HttpHeaders.UserAgent, "RaceTimeApp/$version") }
        }
    }
    single { JolpicaGateway(get(), get(), get()) }
}
val databaseModule = module {
    single { androidContext().getSharedPreferences("racetime_app_cache", Context.MODE_PRIVATE) }
    single<Clock> { Clock.systemUTC() }
    single<ResponseCache> { PreferenceResponseCache(get()) }
    single { mayckgomes.com.racetimeapp.notifications.ReminderScheduler(androidContext(), get()) }
    single { mayckgomes.com.racetimeapp.data.local.FavoritesStore(androidContext()) }
}
val repositoryModule = module { single { RacingRepository(get(), get()) } }
val viewModelModule = module {
    viewModel {
        mayckgomes.com.racetimeapp.presentation.driver.DriverDetailsViewModel(get(), get(), get())
    }
    viewModel { mayckgomes.com.racetimeapp.presentation.home.HomeViewModel(get(), get(), get(), get()) }
    viewModel {
        mayckgomes.com.racetimeapp.presentation.favorites.FavoritesViewModel(get(), get(), get())
    }
    viewModel {
        mayckgomes.com.racetimeapp.presentation.championship.ChampionshipViewModel(get(), get())
    }
    viewModel { mayckgomes.com.racetimeapp.presentation.gp.GrandPrixViewModel(get(), get(), get()) }
    viewModel { mayckgomes.com.racetimeapp.presentation.circuits.CalendarViewModel(get(), get()) }
}
val appModule = listOf(networkModule, databaseModule, repositoryModule, viewModelModule)
