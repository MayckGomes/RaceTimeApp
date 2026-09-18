package mayckgomes.com.racetimeapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class JolpicaApiClient(private val client: HttpClient) {
    suspend fun getCurrentDriversStandings(): Map<String, Any> {
        return client.get("https://api.jolpi.ca/ergast/f1/current/driverstandings.json").body()
    }
}
