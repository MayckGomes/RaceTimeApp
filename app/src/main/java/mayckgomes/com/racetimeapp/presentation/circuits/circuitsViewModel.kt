package mayckgomes.com.racetimeapp.presentation.circuits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

class circuitsViewModel : ViewModel(){

    private val api = Api()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }

    private val _circuitsList = MutableStateFlow(emptyList<RacesCalendar>())
    val circuitsList = _circuitsList.asStateFlow()

    fun getCircuits(){

        viewModelScope.launch{

            _circuitsList.value = emptyList()

            isLoadingTrue()

            _circuitsList.value = api.getCalendar()

            isLoadingFalse()
        }


    }

    private val _actualWeekend = MutableStateFlow("")
    val actualWeekend = _actualWeekend.asStateFlow()

    fun getActualWeekend(){

        viewModelScope.launch {

            val response = api.getCalendar()

            val formater = SimpleDateFormat("yyyy-MM-dd")

            val localDate = formater.parse(LocalDate.now().toString())!!

            for (date in response){

                val circuitDate = formater.parse(date.date)!!

                if (circuitDate >= localDate){

                    _actualWeekend.value = date.date
                    break
                }

            }
        }
    }

    init {
        getCircuits()
        getActualWeekend()
    }

}