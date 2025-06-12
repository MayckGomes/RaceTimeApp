package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.driversQualiResultTable.DriversQualiTable
import mayckgomes.com.racetimeapp.components.driversResultTable.DriversTable
import mayckgomes.com.racetimeapp.domain.models.LastDriverPosition
import mayckgomes.com.racetimeapp.domain.models.QualiDriverPosition

@Composable
fun HomeScreenContent(
    isQuali: Boolean,
    navController: NavController,
    circuitRaceName: String,
    circuitQualiRaceName: String,
    lastRaceResults: List<LastDriverPosition>,
    lastQualiResults: List<QualiDriverPosition>
){

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        if (isQuali){
            Spacer(Modifier.size(20.dp))

            Text(
                text = circuitQualiRaceName,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.size(15.dp))

            DriversQualiTable(navController, lastQualiResults)
        } else {

            Spacer(Modifier.size(20.dp))

            Text(
                text = circuitRaceName,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.size(20.dp))

            DriversTable(navController,lastRaceResults)

        }


    }

}