package mayckgomes.com.racetimeapp.presentation.drivers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.domain.models.DriverPosition

class driversViewmodel : ViewModel(){

    private val api = Api()

    private val _listDrivers = MutableStateFlow(emptyList<DriverPosition>())
    val listDrivers = _listDrivers.asStateFlow()

    fun loadListDrivers(){
        viewModelScope.launch(Dispatchers.IO) {

            isLoadingTrue()
            _listDrivers.value = api.getDriverStandings()
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
        loadListDrivers()
    }
}