package mayckgomes.com.racetimeapp.presentation.drivers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.driversStandingsTable.DriversStandingsTable
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriversScreen(navControler: NavHostController) {
    RaceTimeAppTheme {

        val viewmodel = viewModel<driversViewmodel>()

        val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

        val driverList = viewmodel.listDrivers.collectAsStateWithLifecycle().value

        if (isLoading){

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {

                CircularProgressIndicator()

            }

        } else {

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {
                    viewmodel.loadListDrivers()
                }
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .systemBarsPadding()
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    Text(
                        modifier = Modifier.align(Alignment.Start),
                        text = stringResource(R.string.driverStandings),
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                    )

                    Spacer(Modifier.size(50.dp))


                    DriversStandingsTable(navControler,driverList)

                }

            }

        }

    }
}

@Preview(showSystemUi = true)
@Composable
fun DriversScreenPreview(){
    DriversScreen(rememberNavController())
}