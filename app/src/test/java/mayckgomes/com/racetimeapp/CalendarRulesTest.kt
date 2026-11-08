package mayckgomes.com.racetimeapp

import java.time.*
import mayckgomes.com.racetimeapp.domain.models.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class CalendarRulesTest {
    private val now = Instant.parse("2026-04-09T12:00:00Z")
    private val race =
        RacesCalendar(
            "1",
            "Fixture GP",
            "2026-04-09",
            "14:00:00Z",
            "2026",
            FirstPractice = SessionTime("2026-04-07", "12:00:00Z"),
            Qualifying = SessionTime("2026-04-08", "15:00:00Z"),
        )
    private val session
        get() = race.sessions().single { it.type == SessionType.RACE }

    private fun availability(
        block: DataBlock<List<PublishedResult>>?,
        at: Instant = now,
        loading: Boolean = false,
    ) = resultAvailability(SessionType.RACE, session, block, loading, true, at)

    @Test
    fun resultSelectorOnlyContainsSupportedSessionsPresentInTheSchedule() {
        assertEquals(
            setOf(SessionType.QUALIFYING, SessionType.RACE),
            supportedResultTypes(race, emptySet(), true).toSet(),
        )
        assertEquals(emptyList<SessionType>(), supportedResultTypes(race, emptySet(), false))
    }

    @Test
    fun historicalShootoutIsAScheduleSessionAndNotAFullResultEndpoint() {
        val sprint =
            race.copy(
                SprintShootout = SessionTime("2026-04-08", "10:00:00Z"),
                Sprint = SessionTime("2026-04-08", "12:00:00Z"),
            )
        assertTrue(sprint.sessions().any { it.type == SessionType.SPRINT_QUALIFYING })
        assertFalse(
            supportedResultTypes(sprint, emptySet(), true).contains(SessionType.SPRINT_QUALIFYING)
        )
    }

    @Test
    fun futureAndUnsupportedSessionsDoNotTriggerResultQueries() {
        assertFalse(canQueryResult(session, now))
        assertTrue(canQueryResult(session, now.plusSeconds(7200)))
        assertFalse(canQueryResult(race.sessions().first { it.type == SessionType.FP1 }, now))
    }

    @Test
    fun missingTimesUseTheCalendarDateWithoutInventingAnInstant() {
        val unknown = session.copy(time = null)
        assertNull(unknown.start)
        assertTrue(canQueryResult(unknown, now))
        assertFalse(canQueryResult(unknown.copy(date = "2026-04-10"), now))
        assertFalse(canQueryResult(unknown.copy(date = "invalid"), now))
    }

    @Test
    fun emptyResponseNetworkFailureAndFutureAreDifferentStates() {
        val later = now.plusSeconds(10800)
        assertEquals(ResultAvailability.SCHEDULED, availability(null))
        assertEquals(
            ResultAvailability.AWAITING_PUBLICATION,
            availability(DataBlock(emptyList()), later),
        )
        assertEquals(
            ResultAvailability.REQUEST_FAILED,
            availability(DataBlock(emptyList(), failed = true), later),
        )
        assertEquals(ResultAvailability.LOADING, availability(null, later, true))
    }

    @Test
    fun anUnqueriedPastSessionIsNotDeclaredUnpublished() {
        assertEquals(ResultAvailability.NOT_CHECKED, availability(null, now.plusSeconds(10800)))
    }

    @Test
    fun cachedPublishedResultsRemainAvailableDuringAFailedUpdate() {
        val result =
            PublishedResult(
                "2026",
                "1",
                "Fixture GP",
                "2026-04-09",
                SessionType.RACE,
                listOf(ResultEntry(position = "1", driverId = "fixture")),
            )
        assertEquals(
            ResultAvailability.AVAILABLE,
            availability(DataBlock(listOf(result), stale = true, failed = true)),
        )
    }

    @Test
    fun anElapsedDateIsNotProofOfAPublishedRaceResult() {
        val later = now.plusSeconds(2 * 86400)
        val resolver = WeekendResolver(Clock.fixed(later, ZoneOffset.UTC))
        assertEquals(
            CalendarEventStatus.PAST_SCHEDULE,
            calendarEventStatus(race, resolver.resolve(Season(2026, listOf(race))), later),
        )
        assertEquals(
            CalendarEventStatus.RESULT_PUBLISHED,
            calendarEventStatus(
                race,
                resolver.resolve(Season(2026, listOf(race)), setOf("2026/1/RACE")),
                later,
            ),
        )
    }

    @Test
    fun identicalRoundNumbersFromDifferentYearsAreNotTheSameEvent() {
        val context =
            WeekendContext(
                WeekendState.BETWEEN_WEEKENDS,
                null,
                null,
                null,
                null,
                null,
                race.copy(season = "2027"),
            )
        assertEquals(CalendarEventStatus.UPCOMING, calendarEventStatus(race, context, now))
    }

    @Test
    fun linksOnlyAcceptWikipediaHttpUrlsWithoutCredentials() {
        assertNotNull(wikipediaUrl("https://en.wikipedia.org/wiki/Fixture"))
        assertNull(wikipediaUrl("javascript:alert(1)"))
        assertNull(wikipediaUrl("https://wikipedia.org.example.com/wiki/Fixture"))
        assertNull(wikipediaUrl("https://user:pass@en.wikipedia.org/wiki/Fixture"))
        assertNull(wikipediaUrl(null))
    }
}
