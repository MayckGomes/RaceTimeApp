package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class driversStadingsResponse(
    val MRData: MRDataDriverStandings
)

@Serializable
@SerialName("MRData")
data class MRDataDriverStandings(
    val StandingsTable: StandingsTableDriverStandings
)

@Serializable
@SerialName("StandingsTable")
data class StandingsTableDriverStandings(
    val season: String,
    val round: String,
    val StandingsLists: List<StandingsListsDriverStandings>
)

@Serializable
@SerialName("StandingsLists")
data class StandingsListsDriverStandings(
    val season: String,
    val round: String,
    val DriverStandings : List<DriverPosition>
)

@Serializable
data class DriverPosition(
    val position: String,
    val points: String,
    val wins: String,
    val Driver: Driver,
    val Constructors: List<Constructors>
)

@Serializable
data class Driver(
    val driverId : String,
    val givenName: String,
    val familyName: String
)


val listDriversTest = listOf(
    DriverPosition(
        position = "1",
        points = "25",
        wins = "1",
        Driver = Driver(
            givenName = "kimi",
            familyName = "antonelli",
            driverId = "kimi_antonelli"
        ),
        Constructors = listOf(
            Constructors(
                constructorId = "mercedes",
                name = "mercedes"
            )
        )
    ),
    DriverPosition(
        position = "1",
        points = "25",
        wins = "1",
        Driver = Driver(
            givenName = "kimi",
            familyName = "antonelli",
            driverId = "kimi_antonelli"
        ),
        Constructors = listOf(
            Constructors(
                constructorId = "mercedes",
                name = "mercedes"
            )
        )
    ),
    DriverPosition(
        position = "1",
        points = "25",
        wins = "1",
        Driver = Driver(
            givenName = "kimi",
            familyName = "antonelli",
            driverId = "kimi_antonelli"
        ),
        Constructors = listOf(
            Constructors(
                constructorId = "mercedes",
                name = "mercedes"
            )
        )
    ),
    DriverPosition(
        position = "1",
        points = "25",
        wins = "1",
        Driver = Driver(
            givenName = "kimi",
            familyName = "antonelli",
            driverId = "kimi_antonelli"
        ),
        Constructors = listOf(
            Constructors(
                constructorId = "mercedes",
                name = "mercedes"
            )
        )
    )
)