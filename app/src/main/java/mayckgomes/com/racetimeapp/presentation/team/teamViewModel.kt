package mayckgomes.com.racetimeapp.presentation.team

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.data.repository.teamsRepository
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings

class teamViewModel: ViewModel() {

    private val api = Api()

    private val _teamInfo = MutableStateFlow(emptyList<ConstructorStandings>())
    val teamInfo = _teamInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }


    fun getInfo(id: String){
        viewModelScope.launch {
            isLoadingTrue()
            _teamInfo.value = api.getTeamInfo(id)
            isLoadingFalse()
        }

    }

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    fun isFavoriteTrue(){
        _isFavorite.value = true
    }

    fun isFavoriteFalse(){
        _isFavorite.value = false
    }

    fun verifyFav(context: Context, teamId: String){

        val repository = teamsRepository(context)

        if (repository.verifyTeam(teamId) == null){

            isFavoriteFalse()

        } else {

            isFavoriteTrue()

        }
    }

    fun saveFav(context: Context, teamid: String){

        val repository = teamsRepository(context)

        repository.addFavTeam(teamid,teamid)

    }

    fun delFav(context: Context, teamid: String){

        val repository = teamsRepository(context)

        repository.delTeam(teamid)

    }



}