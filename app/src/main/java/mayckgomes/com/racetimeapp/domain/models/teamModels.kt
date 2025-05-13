package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamsStandingsResponse(
    val MRData: MRDataTeamStandings
)

@Serializable
@SerialName("MRData")
data class MRDataTeamStandings(
    val StandingsTable: TeamsStandingsTable
)

@Serializable
@SerialName("StandingsTable")
data class TeamsStandingsTable(
    val StandingsLists: List<TeamStandingsList>
)

@Serializable
@SerialName("StandingsLists")
data class TeamStandingsList(
    val ConstructorStandings : List<ConstructorStandings>
)

@Serializable
data class ConstructorStandings(
    val position : String,
    val points : String,
    val Constructor: Constructors
)

@Serializable
data class Constructors(
    val constructorId: String,
    val name: String
)