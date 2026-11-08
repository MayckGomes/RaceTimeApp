package mayckgomes.com.racetimeapp

import java.time.Instant
import mayckgomes.com.racetimeapp.data.repository.RacingData
import mayckgomes.com.racetimeapp.domain.models.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class HomePresentationTest {
    private val race =
        RacesCalendar(
            "1",
            "Fixture GP",
            "2026-04-09",
            "14:00:00Z",
            "2026",
            Sprint = SessionTime("2026-04-08", "10:00:00Z"),
            Qualifying = SessionTime("2026-04-08", "15:00:00Z"),
        )

    private fun result(type: SessionType, round: String = "1") =
        PublishedResult(
            "2026",
            round,
            "Fixture GP",
            "2026-04-09",
            type,
            listOf(ResultEntry(position = "1", driverId = "stable", name = "Fixture")),
        )

    private fun data(results: Map<String, DataBlock<List<PublishedResult>>>) =
        RacingData(2026, calendar = DataBlock(listOf(race)), results = results)

    private fun snapshots(data: RacingData) = homeResults(data.calendar.data, data.results.values)

    @Test
    fun choosesLatestActualSessionWithoutDependingOnMapInsertionOrder() {
        val result =
            snapshots(
                data(
                    mapOf(
                        "race" to DataBlock(listOf(result(SessionType.RACE))),
                        "qualifying" to DataBlock(listOf(result(SessionType.QUALIFYING))),
                        "sprint" to DataBlock(listOf(result(SessionType.SPRINT))),
                    )
                )
            )
        assertEquals(
            listOf(SessionType.RACE, SessionType.QUALIFYING, SessionType.SPRINT),
            result.map { it.result.type },
        )
    }

    @Test
    fun lastAliasAndNumberedRoundDoNotDuplicateTheResult() {
        val race = result(SessionType.RACE)
        val result =
            snapshots(
                data(
                    mapOf(
                        "last" to DataBlock(listOf(race), stale = true, updatedAt = 1),
                        "number" to DataBlock(listOf(race), updatedAt = 2),
                    )
                )
            )
        assertEquals(1, result.size)
        assertFalse(result.single().stale)
        assertEquals(2L, result.single().updatedAt)
    }

    @Test
    fun emptyUnpublishedResultsNeverBecomePodiumCards() {
        val result =
            snapshots(
                data(
                    mapOf(
                        "empty" to
                            DataBlock(listOf(result(SessionType.RACE).copy(entries = emptyList())))
                    )
                )
            )
        assertTrue(result.isEmpty())
    }

    @Test
    fun qualifyingInNextGrandPrixIsNewerThanPreviousRace() {
        val result =
            snapshots(
                data(
                    mapOf(
                        "old" to DataBlock(listOf(result(SessionType.RACE))),
                        "new" to DataBlock(listOf(result(SessionType.QUALIFYING, "2"))),
                    )
                )
            )
        assertEquals("2", result.first().result.round)
    }

    @Test
    fun countdownNeverCreatesANegativeValueOrAFalseKnownTime() {
        val now = Instant.parse("2026-04-09T14:00:00Z")
        assertNull(secondsUntil(null, now))
        assertNull(secondsUntil(now.minusSeconds(1), now))
        assertNull(secondsUntil(now, now))
        assertEquals(30L, secondsUntil(now.plusSeconds(30), now))
    }

    @Test
    fun heroDoesNotDuplicateTheSameNextGrandPrix() {
        val context =
            WeekendContext(WeekendState.UPCOMING_WEEKEND, race, null, null, null, null, race)
        assertFalse(shouldShowNextRace(context))
        assertTrue(shouldShowNextRace(context.copy(nextRace = race.copy(round = "2"))))
        assertTrue(shouldShowNextRace(context.copy(nextRace = race.copy(season = "2027"))))
    }

    @Test
    fun sessionDateUsesQualifyingScheduleRatherThanRaceDate() {
        assertEquals(
            "2026-04-08",
            resultSessionDate(data(emptyMap()).calendar.data, result(SessionType.QUALIFYING)),
        )
        assertNull(
            resultSessionDate(data(emptyMap()).calendar.data, result(SessionType.QUALIFYING, "2"))
        )
    }
}
