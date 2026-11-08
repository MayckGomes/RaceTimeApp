package mayckgomes.com.racetimeapp.presentation.championship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.local.FavoritesStore
import mayckgomes.com.racetimeapp.data.repository.RacingRepository

class ChampionshipViewModel(private val repository: RacingRepository, favorites: FavoritesStore) :
    ViewModel() {
    val data = repository.data
    val favorites = favorites.ids
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private var job: Job? = null

    fun refresh(force: Boolean = false) {
        if (job?.isActive == true) return
        job =
            viewModelScope.launch {
                _refreshing.value = true
                try {
                    repository.refreshChampionship(force = force, includeHistory = true)
                } finally {
                    _refreshing.value = false
                }
            }
    }
}
