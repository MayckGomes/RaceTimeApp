package mayckgomes.com.racetimeapp.presentation.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.data.repository.driversRepository
import mayckgomes.com.racetimeapp.data.repository.teamsRepository
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.presentation.drivers.driversViewmodel
import mayckgomes.com.racetimeapp.presentation.teams.teamsViewModel

class favoritesViewmodel: ViewModel() {

    private val api = Api()
    private val driversVM = driversViewmodel()
    private val teamsVM = teamsViewModel()


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

        _listDrivers.value = emptyList()

    }



    private val _listTeams = MutableStateFlow(emptyList<ConstructorStandings>())
    val listTeams = _listTeams.asStateFlow()

    fun addTeam(team: ConstructorStandings){

        _listTeams.value = (_listTeams.value + team).sortedBy { it.position.toInt() }

    }

    fun clearTeams(){

        _listTeams.value = emptyList<ConstructorStandings>()

    }


    suspend fun getDriversFav(context:Context){

        clearDrivers()

        val repository = driversRepository(context)

        val listId = mutableListOf<String>()

        if (repository.getFavDriver().keys.isNotEmpty()){

            repository.getFavDriver().keys.forEach {
                listId.add(it)
            }
        }

        if (driversVM.listDrivers.value.isNotEmpty()){

            val drivers = driversVM.listDrivers.value

            drivers.forEach {

                if (it.Driver.driverId in listId){

                    addDriver(it)

                }

            }

        } else {

            val drivers = api.getDriverStandings()

            viewModelScope.launch {

                driversVM.loadListDrivers()

            }

            drivers.forEach {

                if (it.Driver.driverId in listId){

                    addDriver(it)

                }

            }

        }

    }

    suspend fun getTeamsFav(context:Context){

        clearTeams()

        val repository = teamsRepository(context)

        val listId = mutableListOf<String>()

        if (repository.getFavTeam().keys.isNotEmpty()){
            repository.getFavTeam().keys.forEach {
                listId.add(it)
            }
        }

        if (teamsVM.teamsList.value.isNotEmpty()){

            val teams = teamsVM.teamsList.value

            teams.forEach {

                if (it.Constructor.constructorId in listId){

                    addTeam(it)

                }

            }

        } else {

            val teams = api.getTeamsStandings()

            viewModelScope.launch {

                teamsVM.getTeamsStandings()

            }

            teams.forEach {

                if (it.Constructor.constructorId in listId){

                    addTeam(it)

                }

            }

        }

    }

    fun loadAllData(context: Context){
        viewModelScope.launch {

            isLoadingTrue()

            val drivers = async {
            getDriversFav(context)
            }
            val teams = async {
            getTeamsFav(context)
            }

            drivers.await()
            teams.await()

            isLoadingFalse()

        }
    }
}