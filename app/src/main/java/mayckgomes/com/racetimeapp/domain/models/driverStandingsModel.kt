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
    val StandingsLists: List<StandingsLists>
)

@Serializable
data class StandingsLists(
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