package mayckgomes.com.racetimeapp

import java.time.Instant
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.domain.models.*
import org.junit.Assert.*
import org.junit.Test

class SessionRemindersTest {
    private val now = Instant.parse("2026-11-05T00:00:00Z")
    private val race =
        RacesCalendar(
            "20",
            "Grand Prix",
            "2026-11-08",
            "14:00:00Z",
            "2026",
            FirstPractice = SessionTime("2026-11-06", "12:00:00Z"),
            Qualifying = SessionTime("2026-11-07", "16:00:00Z"),
            SprintQualifying = SessionTime("2026-11-06", "16:00:00Z"),
            Sprint = SessionTime("2026-11-07", "12:00:00Z"),
        )

    @Test
    fun disabledRemindersDoNotScheduleAnything() {
        assertTrue(buildReminders(listOf(race), ReminderSettings(), now).isEmpty())
    }

    @Test
    fun selectedCategoriesIncludeSprintQualifying() {
        val settings =
            ReminderSettings(
                enabled = true,
                races = false,
                qualifying = true,
                practice = false,
                sprint = false,
            )
        val reminders = buildReminders(listOf(race), settings, now)
        assertEquals(
            setOf("home_qualifying", "home_sprint_qualifying"),
            reminders.map { it.label }.toSet(),
        )
    }

    @Test
    fun offsetsAreSubtractedFromUtcStart() {
        val reminders =
            buildReminders(listOf(race), ReminderSettings(enabled = true, minutesBefore = 30), now)
        val raceReminder = reminders.first { it.label == "home_race" }
        assertEquals(
            Instant.parse("2026-11-08T13:30:00Z").toEpochMilli(),
            raceReminder.triggerMillis,
        )
    }

    @Test
    fun doesNotScheduleLateRemindersOrSessionsWithMissingTimes() {
        val settings = ReminderSettings(enabled = true)
        assertTrue(
            buildReminders(listOf(race), settings, Instant.parse("2026-11-08T13:45:00Z")).isEmpty()
        )
        assertTrue(
            buildReminders(
                    listOf(
                        race.copy(
                            time = null,
                            Qualifying = null,
                            SprintQualifying = null,
                            Sprint = null,
                        )
                    ),
                    settings,
                    now,
                )
                .isEmpty()
        )
    }

    @Test
    fun repeatedCalendarEntriesDoNotDuplicateAlarms() {
        val settings = ReminderSettings(enabled = true, practice = true)
        val single = buildReminders(listOf(race), settings, now)
        assertEquals(single, buildReminders(listOf(race, race), settings, now))
        assertEquals(single.size, single.map { it.id }.toSet().size)
    }

    @Test
    fun changingLeadTimeMovesTheSameReminderWithoutChangingItsIdentity() {
        val first =
            buildReminders(listOf(race), ReminderSettings(enabled = true, minutesBefore = 15), now)
        val second =
            buildReminders(listOf(race), ReminderSettings(enabled = true, minutesBefore = 60), now)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(45 * 60000L, first.last().triggerMillis - second.last().triggerMillis)
    }

    @Test
    fun disablingAllCategoriesCancelsAllPlannedReminders() {
        val settings =
            ReminderSettings(
                enabled = true,
                races = false,
                qualifying = false,
                practice = false,
                sprint = false,
            )
        assertTrue(buildReminders(listOf(race), settings, now).isEmpty())
    }
}
