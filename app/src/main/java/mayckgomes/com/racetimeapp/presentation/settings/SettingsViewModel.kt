package mayckgomes.com.racetimeapp.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mayckgomes.com.racetimeapp.data.local.*
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.notifications.*

data class SettingsState(
    val settings: ReminderSettings = ReminderSettings(),
    val allowed: Boolean = false,
    val exact: Boolean = false,
    val upcoming: List<SessionReminder> = emptyList(),
    val lastSync: Long = 0,
    val syncFailed: Boolean = false,
    val schedulingFailed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val appearance: Appearance = Appearance.SYSTEM,
)

class SettingsViewModel(
    context: Context,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) : ViewModel() {
    private val app = context.applicationContext
    private val store = ReminderStore(app)
    private val appearance = AppearancePreferences(app)
    private val _state =
        MutableStateFlow(
            SettingsState(settings = store.settings(), appearance = appearance.current())
        )
    val state = _state.asStateFlow()
    private var pendingSettings: ReminderSettings? = null

    private suspend fun reload() {
        val snapshot =
            withContext(Dispatchers.IO) {
                _state.value.copy(
                    allowed = scheduler.canNotify(),
                    exact = scheduler.canScheduleExactly(),
                    upcoming = store.scheduled().filter {
                        it.startMillis + REMINDER_DELIVERY_GRACE > clock.millis()
                    },
                    lastSync = store.lastSync(),
                    syncFailed = store.syncFailed(),
                    schedulingFailed = store.schedulingFailed(),
                    appearance = appearance.current(),
                )
            }
        _state.update {
            snapshot.copy(settings = it.settings, saving = it.saving, saveFailed = it.saveFailed)
        }
    }

    suspend fun observeWhileResumed() {
        withContext(Dispatchers.IO) {
            runCatching { scheduler.rebuild(forceRearm = true) }
                .onFailure { store.markSchedulingFailed(true) }
        }
        // Leitura local; não consulta HTTP nem enfileira workers a cada tick.
        while (true) {
            reload()
            delay(2000)
        }
    }

    fun save(change: (ReminderSettings) -> ReminderSettings) {
        val current = _state.value
        val updatedSettings = change(current.settings).normalized()
        if (updatedSettings == current.settings) return

        pendingSettings = updatedSettings
        _state.update { it.copy(settings = updatedSettings, saving = true, saveFailed = false) }
        if (current.saving) return

        viewModelScope.launch {
            var persistedSettings = current.settings
            try {
                while (true) {
                    val requestedSettings = pendingSettings ?: break
                    pendingSettings = null
                    val (savedSettings, failed) = withContext(Dispatchers.IO + NonCancellable) {
                        saveReminderSettings(requestedSettings, persistedSettings)
                    }
                    persistedSettings = savedSettings
                    // A newer tap already changed the visible settings. Keep it until saved.
                    if (pendingSettings == null) {
                        _state.update { it.copy(settings = savedSettings, saveFailed = failed) }
                    }
                }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun saveReminderSettings(
        settings: ReminderSettings,
        previousSettings: ReminderSettings,
    ): Pair<ReminderSettings, Boolean> {
        return try {
            if (!scheduler.updateSettings(settings)) return previousSettings to true
            ReminderSyncWorker.configure(app, settings.enabled)
            if (
                settings.enabled &&
                    (!previousSettings.enabled ||
                        clock.millis() - store.lastSync() !in 0L until 6 * 3600_000L)
            ) {
                ReminderSyncWorker.refresh(app)
            }
            settings to false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The preference write may have succeeded before alarm scheduling failed.
            store.settings() to true
        }
    }

    fun setAppearance(value: Appearance) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { appearance.save(value) }
            reload()
            _state.update { it.copy(saveFailed = !saved) }
        }
    }

    fun refreshCalendar() {
        ReminderSyncWorker.refresh(app)
    }
}
