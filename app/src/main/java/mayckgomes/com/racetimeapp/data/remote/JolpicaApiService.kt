package mayckgomes.com.racetimeapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import mayckgomes.com.racetimeapp.data.network.ApiResult
import mayckgomes.com.racetimeapp.data.network.safeRequest
import mayckgomes.com.racetimeapp.domain.models.*

class JolpicaApiService(private val client: HttpClient) {

    suspend fun getSeasonCalendar(): ApiResult<SeasonCalendarResponse> =
        safeRequest { client.get("https://api.jolpi.ca/ergast/f1/current/races.json").body<SeasonCalendarResponse>() }

    suspend fun getDriverStandings(): ApiResult<driversStadingsResponse> =
        safeRequest { client.get("https://api.jolpi.ca/ergast/f1/current/driverstandings.json").body<driversStadingsResponse>() }

    suspend fun getConstructorStandings(): ApiResult<TeamsStandingsResponse> =
        safeRequest { client.get("https://api.jolpi.ca/ergast/f1/current/constructorstandings.json").body<TeamsStandingsResponse>() }

    suspend fun getLastResults(): ApiResult<LastResultsResponse> =
        safeRequest { client.get("https://api.jolpi.ca/ergast/f1/current/last/results.json").body<LastResultsResponse>() }

    suspend fun getQualifying(): ApiResult<QualiResultsResponse> =
        safeRequest { client.get("https://api.jolpi.ca/ergast/f1/current/last/qualifying.json").body<QualiResultsResponse>() }
}
