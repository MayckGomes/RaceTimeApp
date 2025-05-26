package mayckgomes.com.racetimeapp.presentation.teams

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings

class teamsViewModel: ViewModel(){

    private val api = Api()

    private val _teamsList = MutableStateFlow(emptyList<ConstructorStandings>())
    val teamsList = _teamsList.asStateFlow()

    fun getTeamsStandings(){

        viewModelScope.launch {
            isLoadingTrue()
            _teamsList.value = api.getTeamsStandings()
            isLoadingFalse()
        }

    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }

    init {
        getTeamsStandings()
    }
}