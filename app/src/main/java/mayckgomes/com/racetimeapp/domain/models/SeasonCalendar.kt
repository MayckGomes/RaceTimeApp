package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class SeasonCalendarResponse(val MRData: MRDataCalendar)

@Serializable @SerialName("MRData") data class MRDataCalendar(val RaceTable: RaceTableCalendar)

@Serializable
@SerialName("RaceTable")
data class RaceTableCalendar(val season: String, val Races: List<RacesCalendar>)

@Serializable
data class RacesCalendar(
    val round: String,
    val raceName: String,
    val date: String,
    val time: String? = null,
    val season: String = "",
    val Circuit: Circuit? = null,
    val FirstPractice: SessionTime? = null,
    val SecondPractice: SessionTime? = null,
    val ThirdPractice: SessionTime? = null,
    val Qualifying: SessionTime? = null,
    val Sprint: SessionTime? = null,
    val SprintQualifying: SessionTime? = null,
    val SprintShootout: SessionTime? = null,
    val url: String? = null,
)

@Serializable data class SessionTime(val date: String, val time: String? = null)
