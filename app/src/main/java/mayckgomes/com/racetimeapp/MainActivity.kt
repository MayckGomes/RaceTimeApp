package mayckgomes.com.racetimeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import mayckgomes.com.racetimeapp.data.repository.TestNetwork
import mayckgomes.com.racetimeapp.navgation.Navgation
import mayckgomes.com.racetimeapp.presentation.drivers.driversViewmodel
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaceTimeAppTheme {

                val repository = TestNetwork()

                Navgation(repository.verifyNetwork(this))
            }
        }
    }
}
