package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QualiResultsResponse(
    val MRData: MRDataQualiResults
)

@Serializable
@SerialName("MRData")
data class MRDataQualiResults(
    val RaceTable: RaceTableQualiResults
)

@Serializable
@SerialName("RaceTable")
data class RaceTableQualiResults(
    val Races: List<QualiCircuits>
)

@Serializable
@SerialName("Circuits")
data class QualiCircuits(
    val raceName: String,
    val Circuit : Circuit,
    val QualifyingResults: List<QualiDriverPosition>
)

@Serializable
@SerialName("DriverPosition")
data class QualiDriverPosition(
    val position: String,
    val Driver: Driver,
    val Constructor: Constructors,
)
