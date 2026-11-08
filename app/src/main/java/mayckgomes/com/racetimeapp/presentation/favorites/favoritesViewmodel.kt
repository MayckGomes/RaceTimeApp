package mayckgomes.com.racetimeapp.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mayckgomes.com.racetimeapp.data.local.FavoritesStore
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.weekend.*

class FavoritesViewModel(
    private val repository: RacingRepository,
    private val store: FavoritesStore,
    private val clock: Clock,
) : ViewModel() {
    val data = repository.data
    val ids = store.ids
    private val _details = MutableStateFlow<Map<String, FavoriteDetails>>(emptyMap())
    val details = _details.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private var refreshJob: Job? = null
    private val jobs = mutableMapOf<String, Job>()
    private val generations = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch {
            ids.collect { saved ->
                val allowed =
                    saved.drivers.keys.map { favoriteKey(it, false) }.toSet() +
                        saved.constructors.keys.map { favoriteKey(it, true) }
                jobs.keys
                    .filter { it !in allowed }
                    .forEach {
                        jobs.remove(it)?.cancel()
                        generations.remove(it)
                    }
                _details.update { it.filterKeys { key -> key in allowed } }
            }
        }
    }

    private fun contains(id: String, constructor: Boolean) =
        if (constructor) id in ids.value.constructors else id in ids.value.drivers

    fun refresh(force: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob =
            viewModelScope.launch {
                _refreshing.value = true
                try {
                    repository.refreshChampionship(force = force, includeHistory = false)
                } finally {
                    _refreshing.value = false
                }
            }
    }

    fun loadDetails(id: String, constructor: Boolean, force: Boolean = false, generation: Int = 0) {
        if (!contains(id, constructor)) return
        val key = favoriteKey(id, constructor)
        if (jobs[key]?.isActive == true) return
        val refreshOnce = generation > (generations[key] ?: 0)
        val shouldForce = force || refreshOnce
        val old = _details.value[key]
        if (
            !shouldForce &&
                old != null &&
                old.year == repository.activeYear &&
                clock.millis() - old.checkedAt in 0L until 600_000L
        )
            return
        generations[key] = maxOf(generation, generations[key] ?: 0)
        jobs[key] =
            viewModelScope.launch {
                _details.update { it + (key to (old ?: FavoriteDetails()).copy(loading = true)) }
                try {
                    val standing =
                        if (constructor)
                            data.value.championship.data.constructors.firstOrNull { it.id == id }
                        else data.value.championship.data.drivers.firstOrNull { it.id == id }
                    val profile: DataBlock<FavoriteProfile?> =
                        if (standing == null || (!constructor && standing.number.isBlank()))
                            repository.favoriteProfile(id, constructor, shouldForce)
                        else old?.profile ?: DataBlock<FavoriteProfile?>(null)
                    if (contains(id, constructor))
                        _details.update {
                            it +
                                (key to
                                    (it[key] ?: FavoriteDetails()).copy(
                                        profile = profile,
                                        loading = true,
                                    ))
                        }
                    if (!contains(id, constructor)) return@launch
                    val recent = repository.favoriteRace(id, constructor, shouldForce)
                    if (contains(id, constructor))
                        _details.update {
                            it +
                                (key to
                                    FavoriteDetails(
                                        profile,
                                        recent,
                                        clock.millis(),
                                        repository.activeYear,
                                    ))
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (contains(id, constructor))
                        _details.update { details ->
                            val current = details[key] ?: FavoriteDetails()
                            details +
                                (key to
                                    current.copy(
                                        profile = current.profile.copy(failed = true),
                                        loading = false,
                                        checkedAt = clock.millis(),
                                        year = repository.activeYear,
                                    ))
                        }
                } finally {
                    _details.update { value ->
                        if (contains(id, constructor))
                            value + (key to (value[key] ?: FavoriteDetails()).copy(loading = false))
                        else value - key
                    }
                }
            }
    }

    suspend fun remove(id: String, constructor: Boolean): Boolean =
        withContext(Dispatchers.IO) { store.remove(id, constructor) }

    suspend fun restore(id: String, label: String, constructor: Boolean): Boolean =
        withContext(Dispatchers.IO) { store.add(id, label, constructor) }
}
