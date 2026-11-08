package mayckgomes.com.racetimeapp.domain.weekend

import java.time.Instant
import kotlinx.serialization.Serializable
import mayckgomes.com.racetimeapp.domain.model.sessionInstant
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar

@Serializable
enum class SessionType(val key: String, val endpoint: String? = null) {
    FP1("home_fp1"),
    FP2("home_fp2"),
    FP3("home_fp3"),
    SPRINT_QUALIFYING("home_sprint_qualifying"),
    SPRINT("home_sprint", "sprint"),
    QUALIFYING("home_qualifying", "qualifying"),
    RACE("home_race", "results"),
}

enum class SessionStatus {
    TIME_UNKNOWN,
    SCHEDULED,
    SCHEDULED_IN_PROGRESS,
    TEMPORALLY_PAST,
    RESULT_PUBLISHED,
}

enum class WeekendState {
    PRE_SEASON,
    BETWEEN_WEEKENDS,
    UPCOMING_WEEKEND,
    ACTIVE_WEEKEND,
    SESSION_IN_PROGRESS,
    AWAITING_RESULTS,
    WEEKEND_COMPLETED,
    SEASON_COMPLETED,
}

data class Session(
    val season: String,
    val round: String,
    val type: SessionType,
    val date: String,
    val time: String?,
) {
    val start: Instant?
        get() = sessionInstant(date, time)

    val key: String
        get() = "$season/$round/${type.name}"
}

typealias RaceWeekend = RacesCalendar

data class Season(val year: Int, val weekends: List<RaceWeekend>)

data class WeekendContext(
    val state: WeekendState,
    val weekend: RaceWeekend?,
    val currentSession: Session?,
    val nextSession: Session?,
    val previousSession: Session?,
    val previousRace: RaceWeekend?,
    val nextRace: RaceWeekend?,
    val published: Set<String> = emptySet(),
)

fun RaceWeekend.sessions(): List<Session> {
    val dates =
        listOf(
            SessionType.FP1 to FirstPractice,
            SessionType.FP2 to SecondPractice,
            SessionType.FP3 to ThirdPractice,
            SessionType.SPRINT_QUALIFYING to (SprintQualifying ?: SprintShootout),
            SessionType.SPRINT to Sprint,
            SessionType.QUALIFYING to Qualifying,
        )
    return (dates.mapNotNull { (type, t) ->
            t?.let { Session(season, round, type, it.date, it.time) }
        } + Session(season, round, SessionType.RACE, date, time))
        .sortedWith(compareBy<Session> { it.date }.thenBy { it.time ?: "23:59:59Z" })
}

@Serializable
data class ResultEntry(
    val position: String = "",
    val positionText: String = "",
    val driverId: String = "",
    val name: String = "",
    val number: String = "",
    val constructorId: String = "",
    val team: String = "",
    val points: String = "",
    val grid: String = "",
    val status: String = "",
    val time: String = "",
    val q1: String = "",
    val q2: String = "",
    val q3: String = "",
    val fastestRank: String = "",
    val fastestTime: String = "",
    val fastestLap: String = "",
    val completedLaps: String = "",
) {
    val classifiedFinish: Boolean
        get() =
            status.trim().equals("Finished", true) ||
                status.trim().matches(Regex("""\+[1-9][0-9]* Laps?""", RegexOption.IGNORE_CASE))

    val positionsGained: Int?
        get() =
            if (classifiedFinish) {
                val start = grid.toIntOrNull()
                val end = position.toIntOrNull()
                if (
                    start != null &&
                        end != null &&
                        start > 0 &&
                        end > 0 &&
                        (positionText.isBlank() || positionText.toIntOrNull() == end)
                )
                    start - end
                else null
            } else null
}

@Serializable
data class PublishedResult(
    val season: String,
    val round: String,
    val raceName: String,
    val date: String,
    val type: SessionType,
    val entries: List<ResultEntry>,
    val circuit: mayckgomes.com.racetimeapp.domain.models.Circuit? = null,
) {
    val key: String
        get() = "$season/$round/${type.name}"
}

@Serializable
data class Standing(
    val id: String,
    val name: String,
    val position: String = "",
    val points: String = "",
    val wins: String = "",
    val team: String = "",
    val constructorId: String = "",
    val number: String = "",
)

data class DataBlock<T>(
    val data: T,
    val loading: Boolean = false,
    val stale: Boolean = false,
    val failed: Boolean = false,
    val updatedAt: Long = 0,
)

data class ChampionshipState(
    val season: String = "",
    val round: String = "",
    val drivers: List<Standing> = emptyList(),
    val constructors: List<Standing> = emptyList(),
    val previousDrivers: List<Standing> = emptyList(),
    val previousConstructors: List<Standing> = emptyList(),
    val driversInfo: ChampionshipStandingInfo = ChampionshipStandingInfo(),
    val constructorsInfo: ChampionshipStandingInfo = ChampionshipStandingInfo(),
)

fun movement(current: Standing, previous: List<Standing>): Int? {
    val before =
        previous.singleOrNull { it.id == current.id }?.position?.toIntOrNull() ?: return null
    val after = current.position.toIntOrNull() ?: return null
    return if (before > 0 && after > 0) before - after else null
}
