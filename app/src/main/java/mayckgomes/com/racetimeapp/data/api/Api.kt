package mayckgomes.com.racetimeapp.data.api

import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.remote.*
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.models.*
import org.koin.core.context.GlobalContext

class Api {
    private val gateway: JolpicaGateway
        get() = GlobalContext.get().get()

    private val repository: RacingRepository
        get() = GlobalContext.get().get()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val year
        get() = repository.activeYear

    suspend fun getDriverStandings(): List<DriverPosition> {
        val rows =
            gateway
                .query("$year/driverstandings", "StandingsTable.StandingsLists", 600_000)
                .rows
                .flatMap { it.items("DriverStandings") }
        return rows.mapNotNull {
            runCatching { json.decodeFromJsonElement(DriverPosition.serializer(), it) }.getOrNull()
        }
    }

    suspend fun getTeamsStandings(): List<ConstructorStandings> {
        val rows =
            gateway
                .query("$year/constructorstandings", "StandingsTable.StandingsLists", 600_000)
                .rows
                .flatMap { it.items("ConstructorStandings") }
        return rows.mapNotNull {
            runCatching { json.decodeFromJsonElement(ConstructorStandings.serializer(), it) }
                .getOrNull()
        }
    }

    suspend fun getLastResults(): List<Circuits> {
        val rows = gateway.query("$year/last/results", "RaceTable.Races", 300_000).rows
        return rows.mapNotNull {
            runCatching { json.decodeFromJsonElement(Circuits.serializer(), it) }.getOrNull()
        }
    }

    suspend fun getDriverInfo(driverId: String): List<DriverInfo> =
        repository.driver(driverId).mapNotNull {
            runCatching { json.decodeFromJsonElement(DriverInfo.serializer(), it) }.getOrNull()
        }

    suspend fun getTeamInfo(teamID: String): List<ConstructorStandings> {
        val current = getTeamsStandings().filter { it.Constructor.constructorId == teamID }
        if (current.isNotEmpty()) return current
        return repository
            .favoriteProfile(teamID, true)
            .data
            ?.let {
                listOf(ConstructorStandings("", "", Constructors(it.id, it.name, it.nationality)))
            }
            .orEmpty()
    }

    suspend fun getCalendar(): List<RacesCalendar> = repository.calendar(year).data

    suspend fun getQualiResults(): List<QualiCircuits> =
        gateway.query("$year/last/qualifying", "RaceTable.Races", 300_000).rows.mapNotNull {
            runCatching { json.decodeFromJsonElement(QualiCircuits.serializer(), it) }.getOrNull()
        }
}
