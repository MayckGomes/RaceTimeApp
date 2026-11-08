package mayckgomes.com.racetimeapp.presentation.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mayckgomes.com.racetimeapp.data.local.FavoritesStore
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.weekend.*

data class DriverDetailsState(
    val id: String = "",
    val profile: DataBlock<FavoriteProfile?> = DataBlock<FavoriteProfile?>(null),
    val recent: DataBlock<PublishedResult?> = DataBlock<PublishedResult?>(null),
    val loading: Boolean = false,
    val checkedAt: Long = 0,
    val savingFavorite: Boolean = false,
    val favoriteFailed: Boolean = false,
)

class DriverDetailsViewModel(
    private val repository: RacingRepository,
    private val favorites: FavoritesStore,
    val clock: Clock,
) : ViewModel() {
    val racing = repository.data
    val favoriteIds = favorites.ids
    private val _state = MutableStateFlow(DriverDetailsState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun load(id: String, force: Boolean = false) {
        if (!id.matches(Regex("[a-zA-Z0-9_]+"))) {
            _state.value =
                DriverDetailsState(id, profile = DataBlock<FavoriteProfile?>(null, failed = true))
            return
        }
        if (_state.value.id == id && job?.isActive == true) return
        if (
            !force &&
                _state.value.id == id &&
                clock.millis() - _state.value.checkedAt in 0L until 600_000L
        )
            return
        job?.cancel()
        if (_state.value.id != id) _state.value = DriverDetailsState(id)
        _state.update { it.copy(loading = true) }
        job =
            viewModelScope.launch {
                try {
                    val profile = repository.favoriteProfile(id, constructor = false, force = force)
                    ensureActive()
                    _state.update { current ->
                        current.copy(
                            profile =
                                if (
                                    profile.failed &&
                                        profile.data == null &&
                                        current.profile.data != null
                                )
                                    current.profile.copy(failed = true, stale = true)
                                else profile
                        )
                    }
                    repository.refreshChampionship(force = force, includeHistory = false)
                    ensureActive()
                    val recent = repository.favoriteRace(id, constructor = false, force = force)
                    ensureActive()
                    _state.update { current ->
                        current.copy(
                            recent =
                                if (
                                    recent.failed &&
                                        recent.data == null &&
                                        current.recent.data != null
                                )
                                    current.recent.copy(failed = true, stale = true)
                                else recent,
                            checkedAt = clock.millis(),
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _state.update {
                        it.copy(
                            profile =
                                if (it.profile.data == null) it.profile.copy(failed = true)
                                else it.profile,
                            recent = it.recent.copy(failed = true),
                            checkedAt = clock.millis(),
                        )
                    }
                } finally {
                    if (isActive && _state.value.id == id)
                        _state.update { it.copy(loading = false) }
                }
            }
    }

    fun toggleFavorite(id: String) {
        if (
            _state.value.id != id ||
                _state.value.savingFavorite ||
                !id.matches(Regex("[a-zA-Z0-9_]+"))
        )
            return
        val remove = id in favoriteIds.value.drivers
        val name =
            _state.value.profile.data?.name?.takeIf(String::isNotBlank)
                ?: racing.value.championship.data.drivers
                    .singleOrNull { it.id == id }
                    ?.name
                    ?.takeIf(String::isNotBlank)
                ?: id
        _state.update { it.copy(savingFavorite = true, favoriteFailed = false) }
        viewModelScope.launch {
            try {
                val saved =
                    withContext(Dispatchers.IO + NonCancellable) {
                        if (remove) favorites.remove(id, false) else favorites.add(id, name, false)
                    }
                _state.update { it.copy(favoriteFailed = !saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(favoriteFailed = true) }
            } finally {
                _state.update { it.copy(savingFavorite = false) }
            }
        }
    }
}
