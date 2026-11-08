package mayckgomes.com.racetimeapp

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.data.local.*
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.notifications.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPreferencesTest {
    private fun isolated(block: (Context) -> Unit) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "test_reminders_${System.nanoTime()}_"
        val names = mutableSetOf<String>()
        val context =
            object : ContextWrapper(base) {
                override fun getApplicationContext(): Context = this

                override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                    names += prefix + name
                    return base.getSharedPreferences(prefix + name, mode)
                }
            }
        try {
            block(context)
        } finally {
            names.forEach { base.deleteSharedPreferences(it) }
        }
    }

    @Test
    fun oldSettingsKeepTheirEnabledCategoriesAndMigrateOnTheNextSave() = isolated { context ->
        val prefs = context.getSharedPreferences("session_reminders", Context.MODE_PRIVATE)
        assertTrue(
            prefs
                .edit()
                .putString(
                    "settings",
                    """{"enabled":true,"practice":true,"qualifying":false,"races":false,"sprint":true,"minutesBefore":60}""",
                )
                .commit()
        )
        val store = ReminderStore(context)
        val old = store.settings()
        assertTrue(old.fp1 && old.fp2 && old.fp3)
        assertFalse(old.sprintQualifying)
        assertFalse(old.races)
        assertTrue(store.saveSettings(old.copy(fp2 = false, minutesBefore = 0)))
        val recreated = ReminderStore(context).settings()
        assertTrue(recreated.fp1)
        assertFalse(recreated.fp2)
        assertTrue(recreated.fp3)
        assertFalse(recreated.qualifying)
        assertFalse(recreated.sprintQualifying)
        assertEquals(0, recreated.minutesBefore)
    }

    @Test
    fun individualChoiceMatchingADependentDefaultSurvivesRecreation() = isolated { context ->
        val settings = ReminderSettings(enabled = true, qualifying = false, sprintQualifying = true)
        assertTrue(ReminderStore(context).saveSettings(settings))
        assertEquals(settings, ReminderStore(context).settings())
    }

    @Test
    fun storedAlarmsAndDeliveryMarkersSurviveRecreation() = isolated { context ->
        val reminder =
            SessionReminder(
                "2026:1:home_race:1",
                "Fixture GP",
                "home_race",
                1000,
                1000,
                "2026",
                "1",
            )
        val store = ReminderStore(context)
        assertTrue(store.saveScheduled(listOf(reminder)))
        assertTrue(store.markDelivered(reminder))
        assertEquals(listOf(reminder), ReminderStore(context).scheduled())
        assertTrue(
            reminderWasDelivered(
                reminder.copy(id = "2026:1:home_race:2", startMillis = 2000),
                ReminderStore(context).delivered(),
            )
        )
    }

    @Test
    fun appearancePersistsWithoutChangingTheFavoriteFiles() = isolated { context ->
        val drivers = context.getSharedPreferences("driversFavorites", Context.MODE_PRIVATE)
        assertTrue(drivers.edit().putString("old", "Original Label").commit())
        assertTrue(AppearancePreferences(context).save(Appearance.DARK))
        assertEquals(Appearance.DARK, AppearancePreferences(context).current())
        assertEquals("Original Label", drivers.getString("old", null))
    }

    @Test
    fun notificationDestinationIncludesTheSeasonAndRound() {
        assertEquals(
            ReminderTarget(2026, "20"),
            reminderTarget(Intent().setData(Uri.parse("racetime://gp/2026/20"))),
        )
        assertEquals(
            ReminderTarget(2025, "3"),
            reminderTarget(Intent().setData(Uri.parse("racetime://gp/2025/3"))),
        )
    }

    @Test
    fun malformedNotificationDestinationsAreIgnored() {
        for (value in
            listOf(
                "https://gp/2026/20",
                "racetime://other/2026/20",
                "racetime://gp/2026/0",
                "racetime://gp/bad/2",
                "racetime://gp/2026/2/extra",
            )) {
            assertNull(reminderTarget(Intent().setData(Uri.parse(value))))
        }
    }
}
