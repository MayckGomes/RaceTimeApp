package mayckgomes.com.racetimeapp

import java.time.Instant
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.domain.models.*
import org.junit.Assert.*
import org.junit.Test

class RaceScheduleTest {
    private fun race(date: String, time: String? = "14:00:00Z") =
        RacesCalendar("1", "Grand Prix", date, time)

    @Test
    fun skipsPastRoundsEvenWhenCalendarIsNotSorted() {
        val future = race("2026-11-08")
        assertEquals(
            future,
            nextRace(listOf(future, race("2026-10-25")), Instant.parse("2026-11-01T12:00:00Z")),
        )
    }

    @Test
    fun usesRaceStartInsteadOfMidnight() {
        val today = race("2026-11-01")
        assertEquals(today, nextRace(listOf(today), Instant.parse("2026-11-01T13:59:59Z")))
        assertNull(nextRace(listOf(today), Instant.parse("2026-11-01T14:00:00Z")))
    }

    @Test
    fun doesNotInventAStartTime() {
        assertNull(sessionInstant("2026-11-01", null))
        assertNull(sessionInstant("invalid", "14:00:00Z"))
        assertNotNull(
            nextRace(listOf(race("2026-11-01", null)), Instant.parse("2026-11-01T15:00:00Z"))
        )
        assertNull(
            nextRace(listOf(race("2026-11-01", null)), Instant.parse("2026-11-02T00:00:00Z"))
        )
    }

    @Test
    fun selectsSprintQualifyingBeforeSprintAndRace() {
        val gp =
            race("2026-11-08")
                .copy(
                    FirstPractice = SessionTime("2026-11-06", "12:00:00Z"),
                    SprintQualifying = SessionTime("2026-11-06", "16:00:00Z"),
                    Sprint = SessionTime("2026-11-07", "14:00:00Z"),
                )
        val next = upcomingSession(gp, Instant.parse("2026-11-06T13:00:00Z"))
        assertEquals("home_sprint_qualifying", next?.label)
        assertEquals(Instant.parse("2026-11-06T16:00:00Z"), next?.start)
    }

    @Test
    fun supportsOldSprintShootoutNameWithoutDuplicatingIt() {
        val gp = race("2026-11-08").copy(SprintShootout = SessionTime("2026-11-06", "16:00:00Z"))
        assertEquals(
            "home_sprint_qualifying",
            upcomingSession(gp, Instant.parse("2026-11-06T13:00:00Z"))?.label,
        )
    }

    @Test
    fun finishedSeasonAndEmptyCalendarHaveNoNextRace() {
        val now = Instant.parse("2027-01-01T00:00:00Z")
        assertNull(nextRace(emptyList(), now))
        assertNull(nextRace(listOf(race("2026-11-08")), now))
    }
}
