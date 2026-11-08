package mayckgomes.com.racetimeapp.presentation.circuits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.weekend.*

data class CalendarUiState(
    val year: Int,
    val calendar: DataBlock<List<RacesCalendar>> = DataBlock(emptyList(), loading = true),
    val seasons: DataBlock<List<Int>> = DataBlock(emptyList(), loading = true),
)

class CalendarViewModel(private val repository: RacingRepository, val clock: Clock) : ViewModel() {
    val activeYear
        get() = repository.activeYear

    private val _state = MutableStateFlow(CalendarUiState(activeYear))
    val state = _state.asStateFlow()
    private var calendarJob: Job? = null
    private var seasonsJob: Job? = null
    val context =
        combine(
                _state,
                repository.data,
                flow {
                    while (true) {
                        emit(clock.instant())
                        delay(1000)
                    }
                },
            ) { state, data, _ ->
                WeekendResolver(clock)
                    .resolve(
                        Season(state.year, state.calendar.data),
                        data.results.values.flatMap { it.data }.map { it.key }.toSet(),
                    )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                WeekendResolver(clock).resolve(Season(activeYear, emptyList())),
            )

    init {
        loadSeasons()
    }

    fun loadSeasons(force: Boolean = false) {
        if (seasonsJob?.isActive == true) return
        seasonsJob =
            viewModelScope.launch {
                _state.update { it.copy(seasons = it.seasons.copy(loading = true)) }
                try {
                    val result = repository.seasons(force)
                    _state.update { it.copy(seasons = result) }
                } finally {
                    _state.update { it.copy(seasons = it.seasons.copy(loading = false)) }
                }
            }
    }

    fun load(year: Int, force: Boolean = false) {
        if (calendarJob?.isActive == true && _state.value.year == year) return
        calendarJob?.cancel()
        _state.update {
            it.copy(
                year = year,
                calendar =
                    if (it.year == year) it.calendar.copy(loading = true)
                    else DataBlock(emptyList(), loading = true),
            )
        }
        calendarJob =
            viewModelScope.launch {
                try {
                    val result = repository.calendar(year, force)
                    ensureActive()
                    if (_state.value.year == year) _state.update { it.copy(calendar = result) }
                } finally {
                    if (isActive && _state.value.year == year)
                        _state.update { it.copy(calendar = it.calendar.copy(loading = false)) }
                }
            }
    }
}
