package mayckgomes.com.racetimeapp.data.repository

import mayckgomes.com.racetimeapp.data.remote.JolpicaApiClient

class SeasonRepository(private val jolpicaApiClient: JolpicaApiClient) {
    fun appVersion(): String = "RaceTimeApp/2.0.0"

    suspend fun currentDriversStandings(): Map<String, Any> {
        return jolpicaApiClient.getCurrentDriversStandings()
    }
}
