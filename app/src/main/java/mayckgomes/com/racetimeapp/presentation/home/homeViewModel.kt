package mayckgomes.com.racetimeapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.domain.models.LastDriverPosition
import mayckgomes.com.racetimeapp.domain.models.QualiDriverPosition

class homeViewmodel: ViewModel(){

    private val api = Api()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }

    private val _circuitName = MutableStateFlow("")
    val circuitName = _circuitName.asStateFlow()

    private val _resultsList = MutableStateFlow(emptyList<LastDriverPosition>())
    val resultsList = _resultsList.asStateFlow()


    private val _circuitQualiName = MutableStateFlow("")
    val circuitQualiName = _circuitQualiName.asStateFlow()

    private val _resultsQualiList = MutableStateFlow(emptyList<QualiDriverPosition>())
    val resultsQualiList = _resultsQualiList.asStateFlow()



    fun getLastResults(){

        viewModelScope.launch {
            isLoadingTrue()

            val race = async{
                val response = api.getLastResults()

                if (response.isNotEmpty()){
                    _circuitName.value = response.first().raceName
                    _resultsList.value = response.first().Results
                }
            }

            val grid = async {

                val response = api.getQualiResults()

                if (response.isNotEmpty()){
                    _circuitQualiName.value = response.first().raceName
                    _resultsQualiList.value = response.first().QualifyingResults
                }

            }

            race.await()
            grid.await()

            isLoadingFalse()
        }

    }

    init {
        getLastResults()
    }

}