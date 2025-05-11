package mayckgomes.com.racetimeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mayckgomes.com.racetimeapp.navgation.Navgation
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaceTimeAppTheme {
                Navgation()
            }
        }
    }
}
