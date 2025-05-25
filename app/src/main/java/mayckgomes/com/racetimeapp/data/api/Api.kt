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
import mayckgomes.com.racetimeapp.domain.models.DriverInfo
import mayckgomes.com.racetimeapp.domain.models.DriverInfoResponse
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.domain.models.DriverPositionResponse
import mayckgomes.com.racetimeapp.domain.models.LastResultsResponse
import mayckgomes.com.racetimeapp.domain.models.QualiCircuits
import mayckgomes.com.racetimeapp.domain.models.QualiResultsResponse
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.models.SeasonCalendarResponse
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

            emptyList()
        }
    }


    suspend fun getTeamsStandings(): List<ConstructorStandings> {

        return try {

            val request: TeamsStandingsResponse = client.get("https://api.jolpi.ca/ergast/f1/current/constructorstandings.json").body()

            request.MRData.StandingsTable.StandingsLists.first().ConstructorStandings

        } catch (e: Exception){
            Log.d("teste", "erro api: $e")
            emptyList()
        }

    }

    suspend fun getLastResults(): List<Circuits> {
        return try {

            Log.d("tentou", "getLastResults: tentou next")
            val request: LastResultsResponse = client
                .get("https://api.jolpi.ca/ergast/f1/current/last/results.json")
                .body()

            Log.d("tentou", "json: ${request}")
            request.MRData.RaceTable.Races

        } catch (e: Exception){
            Log.d("teste", "getLastResults: $e")
            emptyList()
        }
    }

    suspend fun getDriverInfo(driverId: String): List<DriverInfo> {

        return try {


            val response: DriverInfoResponse = client
                .get("https://api.jolpi.ca/ergast/f1/drivers/${driverId}.json")
                .body()


            response.MRData.DriverTable.Drivers

        } catch (e: Exception){

            Log.d("teste", "getDriverInfo: $e")
            emptyList<DriverInfo>()

        }

    }

    suspend fun getDriverResults(driverId: String): DriverPosition {

        val request: DriverPositionResponse = client.get("https://api.jolpi.ca/ergast/f1/current/drivers/$driverId/driverstandings.json").body()

        return request.MRData.StandingsTable.StandingsLists.first().DriverStandings.first()

    }

    suspend fun getTeamInfo(teamID: String): List<ConstructorStandings> {
        return try {

            val request: TeamsStandingsResponse = client.get("https://api.jolpi.ca/ergast/f1/current/constructors/${teamID}/constructorstandings.json").body()

            request.MRData.StandingsTable.StandingsLists.first().ConstructorStandings

        } catch (e: Exception){
            Log.d("teste", "erro api: $e")
            emptyList<ConstructorStandings>()
        }
    }

    suspend fun getCalendar(): List<RacesCalendar> {

        return try {
            val request: SeasonCalendarResponse = client.get("https://api.jolpi.ca/ergast/f1/current/races.json").body()

            request.MRData.RaceTable.Races
        } catch (e: Exception){

            emptyList()
        }
    }


    suspend fun getQualiResults(): List<QualiCircuits>{

        return try {

            val response: QualiResultsResponse = client
                .get("https://api.jolpi.ca/ergast/f1/current/last/qualifying.json")
                .body()

            response.MRData.RaceTable.Races

        } catch (e: Exception){

            emptyList()

        }

    }
}