package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.components.driversResultTable.DriversTable
import mayckgomes.com.racetimeapp.presentation.homeQuali.HomeRaceViewmodel

@Composable
fun HomeRaceScreenContent(
    navController: NavController,
){

    val viewmodel: HomeRaceViewmodel = viewModel()

    val circuitRaceName by viewmodel.circuitName.collectAsStateWithLifecycle()

    val lastRaceResults by viewmodel.lastResults.collectAsStateWithLifecycle()

    val isLoading by viewmodel.isLoading.collectAsStateWithLifecycle()

    if (isLoading){

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ){
            CircularProgressIndicator()
        }

    } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Spacer(Modifier.size(20.dp))

            Text(
                text = circuitRaceName,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.size(20.dp))

            DriversTable(navController, lastRaceResults)

        }
    }

}