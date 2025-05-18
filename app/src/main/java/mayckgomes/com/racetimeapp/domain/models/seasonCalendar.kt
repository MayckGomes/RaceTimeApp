package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class SeasonCalendarResponse(
    val MRData: MRDataCalendar
)

@Serializable
@SerialName("MRData")
data class MRDataCalendar(
    val RaceTable: RaceTableCalendar
)

@Serializable
@SerialName("RaceTable")
data class RaceTableCalendar(
    val season: String,
    val Races: List<RacesCalendar>
)

@Serializable
data class RacesCalendar(
    val round: String,
    val raceName: String,
    val date: String,
)