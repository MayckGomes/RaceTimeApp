package mayckgomes.com.racetimeapp.ui.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mayckgomes.com.racetimeapp.data.repository.SeasonRepository

class TestViewModel(
    seasonRepository: SeasonRepository
) : ViewModel() {
    private val _appVersion = MutableStateFlow(seasonRepository.appVersion())
    val appVersion: StateFlow<String> = _appVersion
}
