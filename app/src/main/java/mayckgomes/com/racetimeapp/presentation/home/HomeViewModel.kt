package mayckgomes.com.racetimeapp.presentation.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mayckgomes.com.racetimeapp.data.network.ApiResult
import mayckgomes.com.racetimeapp.data.remote.JolpicaApiService
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.max

sealed class BlockState<out T> {
    object Loading : BlockState<Nothing>()
    data class Success<T>(val data: T, val isStale: Boolean = false) : BlockState<T>()
    data class Error(val message: String) : BlockState<Nothing>()
}

class HomeViewModel(
    private val api: JolpicaApiService,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _nextRace = MutableStateFlow<BlockState<RacesCalendar>>(BlockState.Loading)
    val nextRace: StateFlow<BlockState<RacesCalendar>> = _nextRace

    private val _lastRace = MutableStateFlow<BlockState<mayckgomes.com.racetimeapp.domain.models.Circuits>>(BlockState.Loading)
        val lastRace: StateFlow<BlockState<mayckgomes.com.racetimeapp.domain.models.Circuits>> = _lastRace

    private val _driversTop = MutableStateFlow<BlockState<List<DriverPosition>>>(BlockState.Loading)
    val driversTop: StateFlow<BlockState<List<DriverPosition>>> = _driversTop

    private val _constructorsTop = MutableStateFlow<BlockState<List<ConstructorStandings>>>(BlockState.Loading)
    val constructorsTop: StateFlow<BlockState<List<ConstructorStandings>>> = _constructorsTop

    private val _countdownSeconds = MutableStateFlow<Long?>(null)
    val countdownSeconds: StateFlow<Long?> = _countdownSeconds
    private var countdownJob: Job? = null

    private val CACHE_VALIDITY_MS = 7L * 24L * 60L * 60L * 1000L

    fun refreshAll(isOnline: Boolean) {
        viewModelScope.launch {
            // launch parallel loads
            val n = launch { loadNextRace(isOnline) }
            val l = launch { loadLastRace(isOnline) }
            val d = launch { loadDrivers(isOnline) }
            val c = launch { loadConstructors(isOnline) }
            // they run in parallel; errors handled per-block
            n.join(); l.join(); d.join(); c.join()
        }
    }

    private suspend fun loadNextRace(isOnline: Boolean) {
        _nextRace.value = BlockState.Loading
        try {
            when (val res = api.getSeasonCalendar()) {
                is ApiResult.Success -> {
                    val races = res.data.MRData.RaceTable.Races
                    val next = races.firstOrNull()
                    if (next != null) {
                        // cache
                        prefs.edit().putString("calendar_json", json.encodeToString(ListSerializer(RacesCalendar.serializer()), races)).putLong("calendar_fetchedAt", System.currentTimeMillis()).apply()
                        _nextRace.value = BlockState.Success(next, false)
                        startCountdownForRace(next)
                        return
                    }
                    _nextRace.value = BlockState.Error("No next race")
                }
                else -> {
                    // fallback to cache
                    val cached = prefs.getString("calendar_json", null)
                    val fetchedAt = prefs.getLong("calendar_fetchedAt", 0L)
                    if (cached != null) {
                        val races = json.decodeFromString(ListSerializer(RacesCalendar.serializer()), cached)
                        val next = races.firstOrNull()
                        val isStale = System.currentTimeMillis() - fetchedAt > CACHE_VALIDITY_MS
                        if (next != null) {
                            _nextRace.value = BlockState.Success(next, isStale)
                            startCountdownForRace(next)
                        } else _nextRace.value = BlockState.Error("Cached data empty")
                    } else {
                        _nextRace.value = BlockState.Error("Network error and no cache")
                    }
                }
            }
        } catch (e: Exception) {
            _nextRace.value = BlockState.Error(e.message ?: "Unknown")
        }
    }

    private suspend fun loadLastRace(isOnline: Boolean) {
        _lastRace.value = BlockState.Loading
        try {
            when (val res = api.getLastResults()) {
                is ApiResult.Success -> {
                    val races = res.data.MRData.RaceTable.Races
                    val last = races.firstOrNull()
                    if (last != null) {
                                            prefs.edit().putString("last_json", json.encodeToString(ListSerializer(mayckgomes.com.racetimeapp.domain.models.Circuits.serializer()), races)).putLong("last_fetchedAt", System.currentTimeMillis()).apply()
                        _lastRace.value = BlockState.Success(last, false)
                        return
                    }
                    _lastRace.value = BlockState.Error("No last race")
                }
                else -> {
                    val cached = prefs.getString("last_json", null)
                    val fetchedAt = prefs.getLong("last_fetchedAt", 0L)
                    if (cached != null) {
                        val races = json.decodeFromString(ListSerializer(mayckgomes.com.racetimeapp.domain.models.Circuits.serializer()), cached)
                        val last = races.firstOrNull()
                        val isStale = System.currentTimeMillis() - fetchedAt > CACHE_VALIDITY_MS
                        if (last != null) {
                            _lastRace.value = BlockState.Success(last, isStale)
                        } else _lastRace.value = BlockState.Error("Cached data empty")
                    } else {
                        _lastRace.value = BlockState.Error("Network error and no cache")
                    }
                }
            }
        } catch (e: Exception) {
            _lastRace.value = BlockState.Error(e.message ?: "Unknown")
        }
    }

    private suspend fun loadDrivers(isOnline: Boolean) {
        _driversTop.value = BlockState.Loading
        try {
            when (val res = api.getDriverStandings()) {
                is ApiResult.Success -> {
                    val list = res.data.MRData.StandingsTable.StandingsLists.firstOrNull()?.DriverStandings ?: emptyList()
                    prefs.edit().putString("drivers_json", json.encodeToString(ListSerializer(mayckgomes.com.racetimeapp.domain.models.DriverPosition.serializer()), list)).putLong("drivers_fetchedAt", System.currentTimeMillis()).apply()
                    _driversTop.value = BlockState.Success(list.take(3), false)
                }
                else -> {
                    val cached = prefs.getString("drivers_json", null)
                    val fetchedAt = prefs.getLong("drivers_fetchedAt", 0L)
                    if (cached != null) {
                        val list = json.decodeFromString(ListSerializer(mayckgomes.com.racetimeapp.domain.models.DriverPosition.serializer()), cached)
                        val isStale = System.currentTimeMillis() - fetchedAt > CACHE_VALIDITY_MS
                        _driversTop.value = BlockState.Success(list.take(3), isStale)
                    } else _driversTop.value = BlockState.Error("Network error and no cache")
                }
            }
        } catch (e: Exception) {
            _driversTop.value = BlockState.Error(e.message ?: "Unknown")
        }
    }

    private suspend fun loadConstructors(isOnline: Boolean) {
        _constructorsTop.value = BlockState.Loading
        try {
            when (val res = api.getConstructorStandings()) {
                is ApiResult.Success -> {
                    val list = res.data.MRData.StandingsTable.StandingsLists.firstOrNull()?.ConstructorStandings ?: emptyList()
                    prefs.edit().putString("constructors_json", json.encodeToString(ListSerializer(mayckgomes.com.racetimeapp.domain.models.ConstructorStandings.serializer()), list)).putLong("constructors_fetchedAt", System.currentTimeMillis()).apply()
                    _constructorsTop.value = BlockState.Success(list.take(3), false)
                }
                else -> {
                    val cached = prefs.getString("constructors_json", null)
                    val fetchedAt = prefs.getLong("constructors_fetchedAt", 0L)
                    if (cached != null) {
                        val list = json.decodeFromString(ListSerializer(mayckgomes.com.racetimeapp.domain.models.ConstructorStandings.serializer()), cached)
                        val isStale = System.currentTimeMillis() - fetchedAt > CACHE_VALIDITY_MS
                        _constructorsTop.value = BlockState.Success(list.take(3), isStale)
                    } else _constructorsTop.value = BlockState.Error("Network error and no cache")
                }
            }
        } catch (e: Exception) {
            _constructorsTop.value = BlockState.Error(e.message ?: "Unknown")
        }
    }

    private fun startCountdownForRace(race: RacesCalendar) {
        // race.date may be YYYY-MM-DD or YYYY-MM-DDThh:mm:ssZ; try parse
        val dateTimeStr = race.date
        val formatter = DateTimeFormatter.ISO_INSTANT
        val targetMillis = try {
            // try parsing as ISO instant
            Instant.parse(dateTimeStr).toEpochMilli()
        } catch (e: Exception) {
            // try as local date
            try {
                val d = java.time.LocalDate.parse(dateTimeStr)
                d.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (e2: Exception) {
                0L
            }
        }
        if (targetMillis <= 0L) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val now = Instant.now().toEpochMilli()
                val seconds = max(0L, (targetMillis - now) / 1000L)
                _countdownSeconds.value = seconds
                if (seconds <= 0L) break
                delay(1000)
            }
        }
    }

    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownSeconds.value = null
    }
}
