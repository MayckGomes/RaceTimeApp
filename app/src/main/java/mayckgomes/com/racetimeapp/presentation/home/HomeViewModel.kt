package mayckgomes.com.racetimeapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mayckgomes.com.racetimeapp.data.local.FavoritesStore
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.notifications.ReminderScheduler

class HomeViewModel(
    private val repository: RacingRepository,
    private val reminderScheduler: ReminderScheduler,
    favoritesStore: FavoritesStore,
    val clock: Clock,
) : ViewModel() {
    val weekendContext = repository.context
    val racingData = repository.data
    val favoriteIds = favoritesStore.ids
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private var job: Job? = null

    init {
        refreshAll(force = false)
    }

    fun refreshAll(force: Boolean = true) {
        if (job?.isActive == true) return
        job =
            viewModelScope.launch {
                _refreshing.value = true
                try {
                    repository.restoreHomeCache()
                    repository.refresh(force = force)
                    val calendarData = repository.data.value.calendar
                    if (!calendarData.failed)
                        withContext(Dispatchers.IO) {
                            reminderScheduler.updateCalendar(
                                calendarData.data,
                                calendarData.updatedAt,
                            )
                        }
                } finally {
                    _refreshing.value = false
                }
            }
    }
}
