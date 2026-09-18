package mayckgomes.com.racetimeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import mayckgomes.com.racetimeapp.data.repository.TestNetwork
import mayckgomes.com.racetimeapp.navgation.Navgation
import mayckgomes.com.racetimeapp.ui.screen.TestViewModel
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaceTimeAppTheme {
                val testViewModel: TestViewModel = koinViewModel()
                testViewModel.appVersion.value

                val repository = TestNetwork()

                MobileAds.initialize(this) {}

                Navgation(repository.verifyNetwork(this))
            }
        }
    }
}
