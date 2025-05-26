package mayckgomes.com.racetimeapp.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.driversStandingsTable.DriversStandingsTable
import mayckgomes.com.racetimeapp.components.teamsStandingsTable.TeamsStandingsTable
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navControler: NavHostController) {

    val viewmodel = viewModel<favoritesViewmodel>()

    val context = LocalContext.current

    val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

    val driverList = viewmodel.listDrivers.collectAsStateWithLifecycle().value

    val teamList = viewmodel.listTeams.collectAsStateWithLifecycle().value

    RaceTimeAppTheme {

        LaunchedEffect(Unit) {
            viewmodel.loadAllData(context)
        }

        if (isLoading){

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                CircularProgressIndicator()

            }

        } else {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())

            ) {

                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = stringResource(R.string.myfavorites),
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                )

                Spacer(Modifier.size(40.dp))

                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = stringResource(R.string.drivers),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = MaterialTheme.typography.titleLarge.fontWeight
                )

                Spacer(Modifier.size(20.dp))

                DriversStandingsTable(navControler, driverList)

                Spacer(Modifier.size(30.dp))

                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = stringResource(R.string.teams),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = MaterialTheme.typography.titleLarge.fontWeight
                )

                Spacer(Modifier.size(20.dp))

                TeamsStandingsTable(navControler, teamList)

            }

        }

    }

}

@Preview(showSystemUi = true)
@Composable
private fun FavoritesScreenPreview() {
    FavoritesScreen(rememberNavController())
}