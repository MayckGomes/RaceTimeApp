package mayckgomes.com.racetimeapp.data.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import mayckgomes.com.racetimeapp.domain.models.DriverPositionDriverStandings
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

    suspend fun getDriverStandings(): List<DriverPositionDriverStandings>{

        return try {

            val request:driversStadingsResponse = client.get("https://api.jolpi.ca/ergast/f1/current/driverstandings.json").body()

            request.MRData.StandingsTable.StandingsLists.first().DriverStandings

        } catch(e: Exception) {

            emptyList<DriverPositionDriverStandings>()
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
}