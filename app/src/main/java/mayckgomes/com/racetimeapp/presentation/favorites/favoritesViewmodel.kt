package mayckgomes.com.racetimeapp.presentation.favorites

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.data.repository.driversRepository
import mayckgomes.com.racetimeapp.data.repository.teamsRepository
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.domain.models.LastDriverPosition
import kotlin.collections.minus
import kotlin.collections.plus

class favoritesViewmodel: ViewModel() {

    private val api = Api()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }

    private val _listDrivers = MutableStateFlow(emptyList<DriverPosition>())
    val listDrivers = _listDrivers.asStateFlow()

    fun addDriver(driver: DriverPosition){

        _listDrivers.value = (_listDrivers.value + driver).sortedBy { it.position.toInt() }

    }

    fun clearDrivers(){

        _listDrivers.value = emptyList<DriverPosition>()

    }



    private val _listTeams = MutableStateFlow(emptyList<ConstructorStandings>())
    val listTeams = _listTeams.asStateFlow()

    fun addTeam(team: ConstructorStandings){

        _listTeams.value = (_listTeams.value + team).sortedBy { it.position.toInt() }

    }

    fun clearTeams(){

        _listTeams.value = emptyList<ConstructorStandings>()

    }


    fun getDriversFav(context:Context){

        clearDrivers()

        val repository = driversRepository(context)

        val listId = mutableListOf<String>()

        if (repository.getFavDriver()?.keys?.isNotEmpty() ?: false){
            repository.getFavDriver()?.keys?.forEach {
                listId.add(it.toString())
            }
        }

        Log.d("list", "list size = ${listId.size}")
        listId.forEach {

            Log.d("list", "no foreach")
            viewModelScope.launch {

                val driver = api.getDriverResults(it)

                addDriver(driver)

            }

        }
    }

    fun getTeamsFav(context:Context){

        clearTeams()

        val repository = teamsRepository(context)

        val listId = mutableListOf<String>()

        if (repository.getFavTeam()?.keys?.isNotEmpty() ?: false){
            repository.getFavTeam()?.keys?.forEach {
                listId.add(it.toString())
            }
        }

        Log.d("list", "list size = ${listId.size}")
        listId.forEach {

            Log.d("list", "no foreach")
            viewModelScope.launch {

                val team = api.getTeamInfo(it)

                addTeam(team.first())

            }

        }

    }

    fun loadAllData(context: Context){
        viewModelScope.launch {

            isLoadingTrue()
            getDriversFav(context)
            getTeamsFav(context)
            isLoadingFalse()

        }

    }

}