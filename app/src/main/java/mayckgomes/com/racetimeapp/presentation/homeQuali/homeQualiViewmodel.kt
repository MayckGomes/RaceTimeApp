package mayckgomes.com.racetimeapp.presentation.homeQuali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.domain.models.QualiDriverPosition

class HomeQualiViewmodel: ViewModel(){

    val api = Api()

    private val _circuitName = MutableStateFlow("")
    val circuitName = _circuitName.asStateFlow()

    private val _lastResults = MutableStateFlow<List<QualiDriverPosition>>(emptyList())
    val lastResults = _lastResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }

    fun getData(){
        viewModelScope.launch {

            isLoadingTrue()

            val request = api.getQualiResults()

            _lastResults.value = request.first().QualifyingResults
            _circuitName.value = request.first().raceName

            isLoadingFalse()

        }
    }

    init {

        getData()

    }
}