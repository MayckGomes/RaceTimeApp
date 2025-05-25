package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class  LastResultsResponse(
    val MRData: MRDataLastResults
)

@Serializable
@SerialName("MRData")
data class MRDataLastResults(
    val RaceTable: RaceTableLastResults
)

@Serializable
@SerialName("RaceTable")
data class RaceTableLastResults(
    val Races: List<Circuits>
)

@Serializable
data class Circuits(
    val raceName: String,
    val Circuit : Circuit,
    val Results: List<LastDriverPosition>
)

@Serializable
data class Circuit(
    val circuitId: String,
    val circuitName: String
)

@Serializable
@SerialName("DriverPosition")
data class LastDriverPosition(
    val position: String,
    val points: String,
    val Driver: Driver,
    val Constructor: Constructors,
    val status: String
)
