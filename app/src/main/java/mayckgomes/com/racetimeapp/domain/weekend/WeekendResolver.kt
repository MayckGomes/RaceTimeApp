package mayckgomes.com.racetimeapp.domain.weekend

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset

class WeekendResolver(private val clock: Clock) {
    fun resolve(
        season: Season,
        publishedResults: Set<String> = emptySet(),
        history: List<RaceWeekend> = emptyList(),
    ): WeekendContext {
        val currentInstant = clock.instant()
        val currentDate = currentInstant.atZone(ZoneOffset.UTC).toLocalDate()
        val raceWeekends = season.weekends.sortedBy { it.date }
        val scheduledSessions = raceWeekends.flatMap { it.sessions() }
        val sessionsWithKnownTime =
            scheduledSessions.filter { it.start != null }.sortedBy { it.start }
        val nextScheduledSession = sessionsWithKnownTime.firstOrNull { it.start!! > currentInstant }
        val previousScheduledSession =
            sessionsWithKnownTime.lastOrNull { it.start!! <= currentInstant }
        val nextRaceWeekend =
            raceWeekends.firstOrNull { race ->
                race
                    .sessions()
                    .lastOrNull { it.type == SessionType.RACE }
                    ?.let {
                        it.start?.isAfter(currentInstant)
                            ?: (runCatching { LocalDate.parse(it.date) >= currentDate }
                                .getOrDefault(false))
                    } == true
            }
        val previousRaceWithResult =
            (raceWeekends + history)
                .filter { "${it.season}/${it.round}/RACE" in publishedResults }
                .maxByOrNull { it.date }
        val activeWeekend =
            raceWeekends.firstOrNull { race ->
                val dates =
                    race.sessions().mapNotNull {
                        runCatching { LocalDate.parse(it.date) }.getOrNull()
                    }
                dates.isNotEmpty() && currentDate >= dates.min() && currentDate <= dates.max()
            }
        val relevantWeekend =
            activeWeekend ?: nextRaceWeekend ?: raceWeekends.lastOrNull() ?: previousRaceWithResult
        val currentSession =
            activeWeekend
                ?.sessions()
                ?.lastOrNull { it.start?.let { start -> start <= currentInstant } == true }
                ?.takeIf { it.date == currentDate.toString() && it.key !in publishedResults }
        val seasonStartDate = raceWeekends.flatMap { it.sessions() }.minOfOrNull { it.date }
        val seasonEndDate = raceWeekends.flatMap { it.sessions() }.maxOfOrNull { it.date }
        val state =
            when {
                raceWeekends.isEmpty() -> WeekendState.PRE_SEASON
                activeWeekend != null &&
                    "${activeWeekend.season}/${activeWeekend.round}/RACE" in publishedResults ->
                    WeekendState.WEEKEND_COMPLETED
                currentSession != null && currentSession.key !in publishedResults ->
                    WeekendState.SESSION_IN_PROGRESS
                activeWeekend != null ->
                    if (
                        previousScheduledSession != null &&
                            previousScheduledSession.date < currentDate.toString() &&
                            previousScheduledSession.type.endpoint != null &&
                            previousScheduledSession.key !in publishedResults
                    )
                        WeekendState.AWAITING_RESULTS
                    else WeekendState.ACTIVE_WEEKEND
                seasonEndDate != null && currentDate.toString() > seasonEndDate ->
                    if (previousRaceWithResult == raceWeekends.last()) WeekendState.SEASON_COMPLETED
                    else WeekendState.AWAITING_RESULTS
                nextScheduledSession != null &&
                    Duration.between(currentInstant, nextScheduledSession.start).toHours() <= 48 ->
                    WeekendState.UPCOMING_WEEKEND
                seasonStartDate != null && currentDate.toString() < seasonStartDate ->
                    WeekendState.PRE_SEASON
                previousScheduledSession != null &&
                    previousScheduledSession.type.endpoint != null &&
                    previousScheduledSession.key !in publishedResults ->
                    WeekendState.AWAITING_RESULTS
                else -> WeekendState.BETWEEN_WEEKENDS
            }
        val previousSession =
            if (currentSession == null) previousScheduledSession
            else sessionsWithKnownTime.lastOrNull { it.start!! < currentSession.start!! }
        return WeekendContext(
            state,
            relevantWeekend,
            currentSession,
            nextScheduledSession,
            previousSession,
            previousRaceWithResult,
            nextRaceWeekend,
            publishedResults,
        )
    }

    fun status(session: Session, weekendContext: WeekendContext): SessionStatus =
        when {
            session.key in weekendContext.published -> SessionStatus.RESULT_PUBLISHED
            session.start == null -> SessionStatus.TIME_UNKNOWN
            session.start!! > clock.instant() -> SessionStatus.SCHEDULED
            weekendContext.currentSession?.key == session.key -> SessionStatus.SCHEDULED_IN_PROGRESS
            else -> SessionStatus.TEMPORALLY_PAST
        }
}
