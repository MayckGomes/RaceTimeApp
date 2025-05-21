package mayckgomes.com.racetimeapp.presentation.network

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mayckgomes.com.racetimeapp.data.repository.TestNetwork

class networkViewmodel: ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun isLoadingTrue(){
        _isLoading.value = true
    }

    fun isLoadingFalse(){
        _isLoading.value = false
    }


}