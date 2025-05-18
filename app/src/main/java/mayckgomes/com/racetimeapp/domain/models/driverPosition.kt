package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriverPositionResponse(
    val MRData: MRdataDriverPosition
)

@Serializable
@SerialName("MRData")
data class MRdataDriverPosition(
    val StandingsTable: StandingsTableDriverPosition
)

@Serializable
@SerialName("StandingsTable")
data class StandingsTableDriverPosition(
    val StandingsLists: List<StandingsLists>
)