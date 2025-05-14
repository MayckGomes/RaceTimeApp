package mayckgomes.com.racetimeapp.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.domain.models.Circuits
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.domain.models.LastResultsResponse
import mayckgomes.com.racetimeapp.domain.models.TeamsStandingsResponse
import mayckgomes.com.racetimeapp.domain.models.driversStadingsResponse

class Api {

    private val client = HttpClient(CIO){
        install(ContentNegotiation){
            json(Json{
                isLenient = true
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun getDriverStandings(): List<DriverPosition>{

        return try {

            val request:driversStadingsResponse = client.get("https://api.jolpi.ca/ergast/f1/current/driverstandings.json").body()

            request.MRData.StandingsTable.StandingsLists.first().DriverStandings

        } catch(e: Exception) {

            emptyList<DriverPosition>()
        }
    }


    suspend fun getTeamsStandings(): List<ConstructorStandings> {

        return try {

            val request: TeamsStandingsResponse = client.get("https://api.jolpi.ca/ergast/f1/current/constructorstandings.json").body()

            request.MRData.StandingsTable.StandingsLists.first().ConstructorStandings

        } catch (e: Exception){
            Log.d("teste", "erro api: $e")
            emptyList<ConstructorStandings>()
        }

    }

    suspend fun getLastResults(): List<Circuits> {
        return try {
            val request: LastResultsResponse = client.get("https://api.jolpi.ca/ergast/f1/current/last/results.json").body()

            request.MRData.RaceTable.Races

        } catch (e: Exception){
            Log.d("teste", "getLastResults: $e")
            emptyList<Circuits>()
        }
    }
}