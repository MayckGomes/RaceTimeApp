package mayckgomes.com.racetimeapp.notifications

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar

class ReminderStore(context: Context) {
    private val prefs = context.getSharedPreferences("session_reminders", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun settings(): ReminderSettings =
        prefs.getString("settings", null)?.let {
            runCatching { json.decodeFromString(ReminderSettings.serializer(), it).normalized() }
                .getOrNull()
        } ?: ReminderSettings()

    fun saveSettings(settings: ReminderSettings): Boolean =
        prefs
            .edit()
            .putString(
                "settings",
                json.encodeToString(ReminderSettings.serializer(), settings.normalized()),
            )
            .commit()

    fun calendar(): List<RacesCalendar> =
        prefs
            .getString("calendar", null)
            ?.let {
                runCatching {
                        json.decodeFromString(ListSerializer(RacesCalendar.serializer()), it)
                    }
                    .getOrNull()
            }
            .orEmpty()

    fun saveCalendar(races: List<RacesCalendar>, updatedAt: Long): Boolean =
        prefs
            .edit()
            .putString(
                "calendar",
                json.encodeToString(ListSerializer(RacesCalendar.serializer()), races),
            )
            .putLong("last_sync", updatedAt)
            .putBoolean("sync_failed", false)
            .commit()

    fun lastSync(): Long = prefs.getLong("last_sync", 0)

    fun syncFailed(): Boolean = prefs.getBoolean("sync_failed", false)

    fun markSyncFailed(failed: Boolean) {
        prefs.edit().putBoolean("sync_failed", failed).commit()
    }

    fun scheduled(): List<SessionReminder> =
        prefs
            .getString("scheduled", null)
            ?.let {
                runCatching {
                        json.decodeFromString(ListSerializer(SessionReminder.serializer()), it)
                    }
                    .getOrNull()
            }
            .orEmpty()

    fun saveScheduled(items: List<SessionReminder>): Boolean =
        prefs
            .edit()
            .putString(
                "scheduled",
                json.encodeToString(ListSerializer(SessionReminder.serializer()), items),
            )
            .commit()

    fun delivered(): Set<String> = prefs.getStringSet("delivered", emptySet()).orEmpty().toSet()

    fun markDelivered(reminder: SessionReminder): Boolean =
        prefs
            .edit()
            .putStringSet("delivered", delivered() + reminder.id + "event:${reminder.eventKey}")
            .commit()

    fun schedulingFailed(): Boolean = prefs.getBoolean("scheduling_failed", false)

    fun markSchedulingFailed(failed: Boolean) {
        prefs.edit().putBoolean("scheduling_failed", failed).commit()
    }
}
