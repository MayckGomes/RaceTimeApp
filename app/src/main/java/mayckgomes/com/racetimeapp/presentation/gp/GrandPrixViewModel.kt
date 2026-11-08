package mayckgomes.com.racetimeapp.presentation.gp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.weekend.*

data class GrandPrixState(
    val race: RacesCalendar? = null,
    val results: Map<SessionType, DataBlock<List<PublishedResult>>> = emptyMap(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val stale: Boolean = false,
    val updatedAt: Long = 0,
    val scheduleAvailable: Boolean = false,
    val resultLoading: Set<SessionType> = emptySet(),
    val pitStops: DataBlock<List<PitStop>>? = null,
    val lapTimes: Map<String, DataBlock<List<LapTiming>>> = emptyMap(),
)

class GrandPrixViewModel(
    private val repository: RacingRepository,
    val clock: Clock,
    private val reminders: mayckgomes.com.racetimeapp.notifications.ReminderScheduler,
) : ViewModel() {
    private val _state = MutableStateFlow(GrandPrixState())
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var selectedKey = ""
    private var pitJob: Job? = null
    private var lapJob: Job? = null
    private var selectedLapDriver: String? = null

    private fun raceResult(season: Int, round: String): PublishedResult? =
        _state.value.results[SessionType.RACE]?.data?.firstOrNull {
            it.season == season.toString() && it.round == round
        }

    fun loadPitStops(season: Int, round: String, force: Boolean = false) {
        if (
            selectedKey != "$season/$round" ||
                !canLoadRaceStatistics(raceResult(season, round), season, round) ||
                !supportsPitStops(season)
        )
            return
        if (pitJob?.isActive == true) return
        val saved = _state.value.pitStops
        if (!force && saved != null) return
        _state.update {
            it.copy(
                pitStops = (saved ?: DataBlock<List<PitStop>>(emptyList())).copy(loading = true)
            )
        }
        pitJob =
            viewModelScope.launch {
                try {
                    val result = repository.pitStops(season, round, force)
                    ensureActive()
                    if (selectedKey == "$season/$round")
                        _state.update {
                            it.copy(
                                pitStops =
                                    if (result.failed && result.data.isEmpty() && saved != null)
                                        saved.copy(stale = true, failed = true, loading = false)
                                    else result
                            )
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (selectedKey == "$season/$round")
                        _state.update {
                            it.copy(
                                pitStops =
                                    (saved ?: DataBlock<List<PitStop>>(emptyList())).copy(
                                        failed = true
                                    )
                            )
                        }
                }
            }
    }

    fun loadLapTimes(season: Int, round: String, driverId: String, force: Boolean = false) {
        val race = raceResult(season, round)
        if (
            selectedKey != "$season/$round" ||
                !canLoadRaceStatistics(race, season, round) ||
                !supportsLapTimes(season) ||
                race?.entries?.any { it.driverId == driverId } != true
        )
            return
        if (lapJob?.isActive == true && selectedLapDriver == driverId) return
        lapJob?.cancel()
        selectedLapDriver?.let { previous ->
            _state.update { value ->
                value.copy(
                    lapTimes =
                        value.lapTimes.mapValues { (id, block) ->
                            if (id == previous) block.copy(loading = false) else block
                        }
                )
            }
        }
        selectedLapDriver = driverId
        val saved = _state.value.lapTimes[driverId]
        if (!force && saved != null) return
        _state.update {
            it.copy(
                lapTimes =
                    it.lapTimes +
                        (driverId to
                            (saved ?: DataBlock<List<LapTiming>>(emptyList())).copy(loading = true))
            )
        }
        lapJob =
            viewModelScope.launch {
                try {
                    val result = repository.lapTimes(season, round, driverId, force)
                    ensureActive()
                    val block =
                        if (result.failed && result.data.isEmpty() && saved != null)
                            saved.copy(stale = true, failed = true, loading = false)
                        else result
                    if (selectedKey == "$season/$round")
                        _state.update { it.copy(lapTimes = it.lapTimes + (driverId to block)) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (selectedKey == "$season/$round")
                        _state.update {
                            it.copy(
                                lapTimes =
                                    it.lapTimes +
                                        (driverId to
                                            (saved ?: DataBlock<List<LapTiming>>(emptyList())).copy(
                                                failed = true
                                            ))
                            )
                        }
                }
            }
    }

    fun load(
        season: Int,
        round: String,
        force: Boolean = false,
        requestedType: SessionType? = null,
    ) {
        require(season > 0 && round.toIntOrNull()?.let { it > 0 } == true)
        val key = "$season/$round"
        if (job?.isActive == true && key == selectedKey) return
        job?.cancel()
        if (selectedKey != key) {
            pitJob?.cancel()
            lapJob?.cancel()
            selectedLapDriver = null
            _state.value = GrandPrixState()
        }
        selectedKey = key
        _state.update { it.copy(loading = true) }
        job =
            viewModelScope.launch {
                try {
                    val metadata = repository.weekend(season, round, force)
                    ensureActive()
                    val shared = repository.data.value
                    if (
                        season == repository.activeYear &&
                            shared.year == season &&
                            shared.calendar.updatedAt > 0 &&
                            !metadata.failed &&
                            !metadata.stale &&
                            !shared.calendar.failed
                    ) {
                        val latest = repository.calendar(season)
                        if (!latest.failed && !latest.stale)
                            withContext(Dispatchers.IO) {
                                reminders.updateCalendar(latest.data, latest.updatedAt)
                            }
                    }
                    val known =
                        repository.data.value.results.values
                            .flatMap { block ->
                                block.data
                                    .filter { it.season == season.toString() && it.round == round }
                                    .map { result ->
                                        result.type to block.copy(data = listOf(result))
                                    }
                            }
                            .groupBy { it.first }
                            .mapValues { (_, copies) ->
                                copies.maxBy { it.second.updatedAt }.second
                            }
                    val old =
                        _state.value.results.filterValues { block ->
                            block.data.any { it.season == season.toString() && it.round == round }
                        }
                    val saved =
                        (old.keys + known.keys).associateWith { type ->
                            listOfNotNull(old[type], known[type]).maxBy { it.updatedAt }
                        }
                    val fallback = saved.values.flatMap { it.data }.firstOrNull()
                    val race =
                        metadata.data
                            ?: fallback?.let {
                                RacesCalendar(
                                    it.round,
                                    it.raceName,
                                    it.date,
                                    season = it.season,
                                    Circuit = it.circuit,
                                )
                            }
                    val types =
                        (metadata.data
                                ?.sessions()
                                ?.filter { canQueryResult(it, clock.instant()) }
                                ?.map { it.type }
                                .orEmpty() +
                                saved.keys +
                                listOfNotNull(
                                    requestedType?.takeIf {
                                        it.endpoint != null && metadata.data == null
                                    }
                                ))
                            .toSet()
                    _state.value =
                        GrandPrixState(
                            race,
                            saved,
                            loading = true,
                            failed = metadata.failed,
                            stale = metadata.stale,
                            updatedAt = metadata.updatedAt,
                            scheduleAvailable = metadata.data != null,
                            resultLoading = types,
                            pitStops = _state.value.pitStops,
                            lapTimes = _state.value.lapTimes,
                        )
                    for (type in types.sortedBy { it.ordinal }) {
                        val fetched = repository.result(season.toString(), round, type, force)
                        ensureActive()
                        val result =
                            if (fetched.failed && fetched.data.isEmpty())
                                saved[type]?.copy(stale = true, failed = true) ?: fetched
                            else fetched
                        val source = result.data.firstOrNull()
                        _state.update {
                            it.copy(
                                results = it.results + (type to result),
                                resultLoading = it.resultLoading - type,
                                race =
                                    it.race
                                        ?: source?.let { published ->
                                            RacesCalendar(
                                                published.round,
                                                published.raceName,
                                                published.date,
                                                season = published.season,
                                                Circuit = published.circuit,
                                            )
                                        },
                            )
                        }
                    }
                    if (force) {
                        if (_state.value.pitStops != null) loadPitStops(season, round, true)
                        selectedLapDriver?.let { loadLapTimes(season, round, it, true) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _state.update { it.copy(failed = true, stale = it.race != null) }
                } finally {
                    if (isActive && selectedKey == key)
                        _state.update { it.copy(loading = false, resultLoading = emptySet()) }
                }
            }
    }
}
