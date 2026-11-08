package mayckgomes.com.racetimeapp

import java.time.*
import mayckgomes.com.racetimeapp.domain.models.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class WeekendResolverTest {
    private val gp =
        RacesCalendar(
            "1",
            "Fixture GP",
            "2026-04-09",
            "14:00:00Z",
            "2026",
            FirstPractice = SessionTime("2026-04-06", "12:00:00Z"),
            SecondPractice = SessionTime("2026-04-07", "12:00:00Z"),
            Qualifying = SessionTime("2026-04-08", "15:00:00Z"),
        )
    private val next =
        gp.copy(
            round = "2",
            date = "2026-04-23",
            FirstPractice = SessionTime("2026-04-20", "12:00:00Z"),
            SecondPractice = null,
            Qualifying = SessionTime("2026-04-22", "15:00:00Z"),
        )

    private fun resolver(at: String, zone: ZoneId = ZoneOffset.UTC) =
        WeekendResolver(Clock.fixed(Instant.parse(at), zone))

    private fun resolve(
        at: String,
        races: List<RacesCalendar> = listOf(gp),
        published: Set<String> = emptySet(),
    ) = resolver(at).resolve(Season(2026, races), published)

    @Test
    fun preseasonUsesActualSessionDatesInsteadOfAssumingFriday() {
        assertEquals(WeekendState.PRE_SEASON, resolve("2026-03-01T00:00:00Z").state)
        assertEquals(SessionType.FP1, resolve("2026-03-01T00:00:00Z").nextSession?.type)
        assertEquals(WeekendState.UPCOMING_WEEKEND, resolve("2026-04-05T12:00:00Z").state)
    }

    @Test
    fun activeWeekendMayStartOnAMonday() {
        assertEquals(WeekendState.ACTIVE_WEEKEND, resolve("2026-04-06T10:00:00Z").state)
        assertEquals(gp, resolve("2026-04-06T10:00:00Z").weekend)
    }

    @Test
    fun reachingScheduledTimeDoesNotClaimConfirmedLiveActivity() {
        val state = resolve("2026-04-09T14:00:00Z")
        assertEquals(WeekendState.SESSION_IN_PROGRESS, state.state)
        assertEquals(SessionType.RACE, state.currentSession?.type)
        assertEquals(
            SessionStatus.SCHEDULED_IN_PROGRESS,
            resolver("2026-04-09T14:00:00Z").status(state.currentSession!!, state),
        )
    }

    @Test
    fun resultPublicationCompletesTheWeekend() {
        val state = resolve("2026-04-09T18:00:00Z", published = setOf("2026/1/RACE"))
        assertEquals(WeekendState.WEEKEND_COMPLETED, state.state)
        assertEquals(gp, state.previousRace)
        assertNull(state.currentSession)
    }

    @Test
    fun passageOfTimeDoesNotInventResultsOrCancellation() {
        val state = resolve("2026-04-10T00:00:00Z")
        assertEquals(WeekendState.AWAITING_RESULTS, state.state)
        assertNull(state.previousRace)
        assertEquals(
            SessionStatus.TEMPORALLY_PAST,
            resolver("2026-04-10T00:00:00Z").status(gp.sessions().last(), state),
        )
    }

    @Test
    fun movesToNextWeekendAndKeepsConfirmedPreviousRace() {
        val state = resolve("2026-04-12T00:00:00Z", listOf(gp, next), setOf("2026/1/RACE"))
        assertEquals(WeekendState.BETWEEN_WEEKENDS, state.state)
        assertEquals(next, state.weekend)
        assertEquals(gp, state.previousRace)
        assertEquals(next, state.nextRace)
    }

    @Test
    fun finalPublishedRoundCompletesTheSeason() {
        val state = resolve("2026-04-10T00:00:00Z", published = setOf("2026/1/RACE"))
        assertEquals(WeekendState.SEASON_COMPLETED, state.state)
        assertNull(state.nextRace)
    }

    @Test
    fun sprintScheduleOnlyIncludesSessionsReturnedByTheApi() {
        val sprint =
            gp.copy(
                SecondPractice = null,
                SprintQualifying = SessionTime("2026-04-07", "12:00:00Z"),
                Sprint = SessionTime("2026-04-08", "12:00:00Z"),
            )
        assertTrue(sprint.sessions().any { it.type == SessionType.SPRINT_QUALIFYING })
        assertFalse(sprint.sessions().any { it.type == SessionType.FP2 })
        assertFalse(gp.sessions().any { it.type == SessionType.SPRINT })
    }

    @Test
    fun historicalShootoutAndNewQualifyingAreNotDuplicated() {
        val legacy = gp.copy(SprintShootout = SessionTime("2026-04-07", "12:00:00Z"))
        assertEquals(1, legacy.sessions().count { it.type == SessionType.SPRINT_QUALIFYING })
        val updated = legacy.copy(SprintQualifying = SessionTime("2026-04-07", "13:00:00Z"))
        assertEquals(
            "13:00:00Z",
            updated.sessions().single { it.type == SessionType.SPRINT_QUALIFYING }.time,
        )
    }

    @Test
    fun unknownTimesDoNotProduceFalseCountdowns() {
        val unknown =
            gp.copy(time = null, FirstPractice = null, SecondPractice = null, Qualifying = null)
        val state = resolve("2026-04-09T10:00:00Z", listOf(unknown))
        assertNull(state.nextSession)
        assertEquals(unknown, state.nextRace)
        assertEquals(
            SessionStatus.TIME_UNKNOWN,
            resolver("2026-04-09T10:00:00Z").status(unknown.sessions().single(), state),
        )
    }

    @Test
    fun changingDeviceZoneDoesNotChangeUtcSelection() {
        val season = Season(2026, listOf(gp))
        val at = "2026-04-09T13:00:00Z"
        assertEquals(
            resolver(at, ZoneId.of("America/Fortaleza")).resolve(season),
            resolver(at, ZoneId.of("Asia/Tokyo")).resolve(season),
        )
    }

    @Test
    fun emptyCalendarIsDistinctFromCompletedSeason() {
        val state = resolve("2026-04-10T00:00:00Z", emptyList())
        assertEquals(WeekendState.PRE_SEASON, state.state)
        assertNull(state.weekend)
    }

    @Test
    fun standingsMovementRequiresValidPositionsAndStableIds() {
        val current = Standing("stable", "Fixture", "2")
        assertEquals(2, movement(current, listOf(current.copy(position = "4"))))
        assertNull(movement(current, emptyList()))
        assertNull(movement(current.copy(position = "D"), listOf(current)))
    }

    @Test
    fun previousSessionDoesNotPointToTheCurrentSession() {
        val state = resolve("2026-04-09T14:00:00Z")
        assertEquals(SessionType.RACE, state.currentSession?.type)
        assertEquals(SessionType.QUALIFYING, state.previousSession?.type)
    }

    @Test
    fun preseasonKeepsTheLastConfirmedRaceFromPreviousYear() {
        val historical = gp.copy(season = "2025", round = "24", date = "2025-12-07")
        val context =
            resolver("2026-03-01T00:00:00Z")
                .resolve(Season(2026, listOf(gp)), setOf("2025/24/RACE"), listOf(historical))
        assertEquals(WeekendState.PRE_SEASON, context.state)
        assertEquals(historical, context.previousRace)
        assertEquals(gp, context.nextRace)
    }

    @Test
    fun aCalendarUpdateMovesTheNextSessionWithoutChangingTheRoundId() {
        val changed = gp.copy(FirstPractice = SessionTime("2026-04-06", "16:00:00Z"))
        val before = resolve("2026-04-05T12:00:00Z")
        val after = resolve("2026-04-05T12:00:00Z", listOf(changed))
        assertEquals(before.nextSession?.key, after.nextSession?.key)
        assertNotEquals(before.nextSession?.start, after.nextSession?.start)
    }
}
