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
    val DriverStandings : List<DriverPositionDriverStandings>
)

@Serializable
@SerialName("DriverPosition")
data class DriverPositionDriverStandings(
    val position: String,
    val points: String,
    val wins: String,
    val Driver: DriverDriverStadings,
    val Constructors: List<Constructors>
)

@Serializable
@SerialName("Driver")
data class DriverDriverStadings(
    val givenName: String,
    val familyName: String
)