package mayckgomes.com.racetimeapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.domain.models.Circuits
import mayckgomes.com.racetimeapp.domain.models.DriverPosition
import mayckgomes.com.racetimeapp.domain.models.LastDriverPosition

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



    fun getLastResults(){

        viewModelScope.launch {
            isLoadingTrue()

            val response = api.getLastResults()

            _circuitName.value = response?.raceName.toString()
            _resultsList.value = response?.Results!!

            isLoadingFalse()
        }

    }

    init {
        getLastResults()
    }

}