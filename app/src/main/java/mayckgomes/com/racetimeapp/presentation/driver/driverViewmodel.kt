package mayckgomes.com.racetimeapp.presentation.driver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.data.api.Api
import mayckgomes.com.racetimeapp.data.repository.driversRepository
import mayckgomes.com.racetimeapp.domain.models.DriverInfo

class driverViewmodel(): ViewModel() {

    private val api = Api()

    private val _driverInfo = MutableStateFlow(emptyList<DriverInfo>())
    val driverInfo = _driverInfo.asStateFlow()

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
            _driverInfo.value = api.getDriverInfo(id)
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

    fun verifyFav(context: Context, driverId: String){

        val repository = driversRepository(context)

        if (repository.verifyDriver(driverId) == null){

            isFavoriteFalse()

        } else {

            isFavoriteTrue()

        }
    }

    fun saveFav(context: Context, driverid: String){

        val repository = driversRepository(context)

        repository.addFavDriver(driverid,driverid)

    }

    fun delFav(context: Context, driverid: String){

        val repository = driversRepository(context)

        repository.delDriver(driverid)

    }
}