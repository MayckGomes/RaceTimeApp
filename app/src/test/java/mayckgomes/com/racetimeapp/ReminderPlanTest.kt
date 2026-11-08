package mayckgomes.com.racetimeapp

import java.time.*
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.domain.models.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPlanTest {
    private val now = Instant.parse("2026-11-05T00:00:00Z")
    private val race =
        RacesCalendar(
            "20",
            "Fixture GP",
            "2026-11-08",
            "14:00:00Z",
            "2026",
            FirstPractice = SessionTime("2026-11-06", "12:00:00Z"),
            SecondPractice = SessionTime("2026-11-06", "16:00:00Z"),
            ThirdPractice = SessionTime("2026-11-07", "11:00:00Z"),
            Qualifying = SessionTime("2026-11-07", "16:00:00Z"),
            SprintQualifying = SessionTime("2026-11-06", "18:00:00Z"),
            Sprint = SessionTime("2026-11-07", "12:00:00Z"),
        )
    private val all = ReminderSettings(enabled = true, practice = true)

    private fun plan(
        settings: ReminderSettings = all,
        races: List<RacesCalendar> = listOf(race),
        existing: List<SessionReminder> = emptyList(),
        at: Instant = now,
        delivered: Set<String> = emptySet(),
        allowed: Boolean = true,
        rearm: Boolean = false,
    ) = planReminders(races, settings, at, existing, delivered, allowed, rearm)

    @Test
    fun oldPracticeAndQualifyingPreferencesMigrateWithoutReenablingDisabledSessions() {
        val old =
            Json.decodeFromString(
                ReminderSettings.serializer(),
                """{"enabled":true,"practice":false,"qualifying":false,"sprint":true,"minutesBefore":15}""",
            )
        assertFalse(old.fp1)
        assertFalse(old.fp2)
        assertFalse(old.fp3)
        assertFalse(old.sprintQualifying)
        assertEquals(15, old.minutesBefore)
        assertTrue(old.sprint)
    }

    @Test
    fun enabledOldPracticeCategoryEnablesAllThreeIndividualPractices() {
        val old =
            Json.decodeFromString(
                ReminderSettings.serializer(),
                """{"practice":true,"qualifying":true}""",
            )
        assertTrue(old.fp1)
        assertTrue(old.fp2)
        assertTrue(old.fp3)
        assertTrue(old.sprintQualifying)
    }

    @Test
    fun independentChoicesSurviveSerializationEvenWhenMatchingTheirDefault() {
        val settings =
            ReminderSettings(
                enabled = true,
                qualifying = false,
                fp1 = true,
                fp2 = false,
                fp3 = true,
                sprintQualifying = true,
            )
        val json = Json { encodeDefaults = true }
        assertEquals(
            settings,
            json.decodeFromString(
                ReminderSettings.serializer(),
                json.encodeToString(ReminderSettings.serializer(), settings),
            ),
        )
    }

    @Test
    fun sevenSessionsCanBeSelectedIndependently() {
        val none =
            all.copy(
                races = false,
                qualifying = false,
                fp1 = false,
                fp2 = false,
                fp3 = false,
                sprint = false,
                sprintQualifying = false,
            )
        val cases =
            listOf(
                none.copy(fp1 = true) to "home_fp1",
                none.copy(fp2 = true) to "home_fp2",
                none.copy(fp3 = true) to "home_fp3",
                none.copy(sprintQualifying = true) to "home_sprint_qualifying",
                none.copy(sprint = true) to "home_sprint",
                none.copy(qualifying = true) to "home_qualifying",
                none.copy(races = true) to "home_race",
            )
        cases.forEach { (settings, expected) ->
            assertEquals(listOf(expected), plan(settings).active.map { it.label })
        }
    }

    @Test
    fun allFourLeadTimesAreSupportedIncludingZero() {
        for (lead in reminderLeadTimes) {
            val reminder = plan(all.copy(minutesBefore = lead)).active.last()
            assertEquals(lead * 60_000L, reminder.startMillis - reminder.triggerMillis)
        }
    }

    @Test
    fun invalidSavedLeadTimeFallsBackToThirtyMinutes() {
        assertEquals(30, all.copy(minutesBefore = -1).normalized().minutesBefore)
        assertEquals(30, all.copy(minutesBefore = 120).normalized().minutesBefore)
    }

    @Test
    fun unchangedCalendarKeepsAlarmsInsteadOfDuplicatingOrCancelingThem() {
        val existing = plan().active
        val next = plan(existing = existing)
        assertTrue(next.cancel.isEmpty())
        assertTrue(next.schedule.isEmpty())
        assertEquals(existing, next.active)
    }

    @Test
    fun bootRearmsSavedAlarmsWithoutChangingTheirIdentity() {
        val existing = plan().active
        val next = plan(existing = existing, rearm = true)
        assertEquals(existing.map { it.id }, next.schedule.map { it.id })
        assertTrue(next.cancel.isEmpty())
    }

    @Test
    fun disablingRemindersCancelsAllPendingAlarms() {
        val existing = plan().active
        val next = plan(all.copy(enabled = false), existing = existing)
        assertEquals(existing, next.cancel)
        assertTrue(next.active.isEmpty())
    }

    @Test
    fun removalOfASessionCancelsOnlyThatSession() {
        val existing = plan().active
        val next = plan(races = listOf(race.copy(SecondPractice = null)), existing = existing)
        assertEquals(listOf("home_fp2"), next.cancel.map { it.label })
        assertTrue(next.schedule.isEmpty())
    }

    @Test
    fun changedTimeCancelsTheOldIdentityAndSchedulesTheNewOne() {
        val existing = plan().active
        val next = plan(races = listOf(race.copy(time = "15:00:00Z")), existing = existing)
        assertEquals(listOf("home_race"), next.cancel.map { it.label })
        assertEquals(listOf("home_race"), next.schedule.map { it.label })
        assertNotEquals(next.cancel.single().id, next.schedule.single().id)
    }

    @Test
    fun changedLeadTimeReplacesTriggerButPreservesSessionIdentity() {
        val existing = plan().active
        val next = plan(all.copy(minutesBefore = 15), existing = existing)
        assertEquals(existing.map { it.id }, next.schedule.map { it.id })
        assertEquals(existing, next.cancel)
    }

    @Test
    fun deliveredSessionDoesNotRepeatAfterAStartTimeChange() {
        val first = plan().active.last()
        val next =
            plan(
                races = listOf(race.copy(time = "15:00:00Z")),
                delivered = setOf("event:${first.eventKey}"),
            )
        assertTrue(next.active.none { it.label == "home_race" })
    }

    @Test
    fun legacyDeliveredIdsAlsoPreventDuplicatesAfterTimeChanges() {
        val first = plan().active.last()
        val next = plan(races = listOf(race.copy(time = "15:00:00Z")), delivered = setOf(first.id))
        assertTrue(next.active.none { it.label == "home_race" })
    }

    @Test
    fun revokedNotificationPermissionCancelsPendingAlarmsWithoutChangingPreferences() {
        val existing = plan().active
        assertEquals(existing, plan(existing = existing, allowed = false).cancel)
        assertTrue(all.enabled)
        assertEquals(existing, plan().active)
    }

    @Test
    fun atStartReminderCanBeDeliveredAtAndJustAfterItsScheduledStart() {
        val settings = all.copy(minutesBefore = 0)
        val reminder = plan(settings).active.last()
        assertTrue(reminderIsDue(reminder, settings, Instant.ofEpochMilli(reminder.startMillis)))
        assertTrue(
            reminderIsDue(reminder, settings, Instant.ofEpochMilli(reminder.startMillis + 1000))
        )
        assertFalse(
            reminderIsDue(reminder, settings, Instant.ofEpochMilli(reminder.startMillis - 1))
        )
    }

    @Test
    fun expiredDeliveryIsDiscardedWithoutAssumingAnOfficialSessionDuration() {
        val reminder = plan().active.last()
        assertFalse(
            reminderIsDue(
                reminder,
                all,
                Instant.ofEpochMilli(reminder.startMillis + REMINDER_DELIVERY_GRACE),
            )
        )
    }

    @Test
    fun aDueExistingAlarmSurvivesRebuildButNoLateNewAlarmIsCreated() {
        val settings = all.copy(minutesBefore = 0)
        val reminder = plan(settings).active.last()
        val at = Instant.ofEpochMilli(reminder.startMillis + 1000)
        assertEquals(
            listOf(reminder),
            plan(settings, existing = listOf(reminder), at = at, rearm = true).active,
        )
        assertTrue(plan(settings, at = at).active.isEmpty())
    }

    @Test
    fun unknownTimesAndInvalidRoundNumbersAreNotScheduled() {
        val unknown =
            race.copy(
                time = null,
                FirstPractice = null,
                SecondPractice = null,
                ThirdPractice = null,
                Sprint = null,
                SprintQualifying = null,
                Qualifying = null,
            )
        assertTrue(plan(races = listOf(unknown)).active.isEmpty())
        assertTrue(plan(races = listOf(race.copy(round = "?"))).active.isEmpty())
    }

    @Test
    fun historicalShootoutUsesTheSameSprintQualifyingPreference() {
        val historical = race.copy(SprintQualifying = null, SprintShootout = race.SprintQualifying)
        assertEquals(plan().active, plan(races = listOf(historical)).active)
    }

    @Test
    fun timeZoneChangesDoNotChangeUtcAlarmInstants() {
        val instant = now.atZone(ZoneId.of("America/Sao_Paulo")).toInstant()
        assertEquals(plan().active, plan(at = instant).active)
    }

    @Test
    fun pendingAlarmCountIsBoundedAndKeepsTheSoonestSessions() {
        val races =
            (1..20).map {
                race.copy(round = it.toString(), date = "2026-11-${it.toString().padStart(2, '0')}")
            }
        val next = plan(races = races)
        assertEquals(64, next.active.size)
        assertEquals(next.active.sortedBy { it.triggerMillis }, next.active)
    }

    @Test
    fun legacyStoredReminderKeepsItsGpDestination() {
        val reminder =
            Json.decodeFromString(
                SessionReminder.serializer(),
                """{"id":"2026:20:home_race:1794146400","raceName":"Fixture GP","label":"home_race","startMillis":1,"triggerMillis":1}""",
            )
        assertEquals(2026 to "20", reminder.destination)
        assertEquals("2026:20:home_race", reminder.eventKey)
    }
}
