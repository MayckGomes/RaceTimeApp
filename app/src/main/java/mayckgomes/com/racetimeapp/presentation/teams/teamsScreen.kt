package mayckgomes.com.racetimeapp.presentation.teams

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
import mayckgomes.com.racetimeapp.components.ads.BannerAdView
import mayckgomes.com.racetimeapp.components.teamsStandingsTable.TeamsStandingsTable
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(navControler: NavHostController) {

    val viewModel = viewModel<teamsViewModel>()

    val teamsList = viewModel.teamsList.collectAsStateWithLifecycle().value

    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    RaceTimeAppTheme {

        if (isLoading){

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize(1f)
            ) {
                CircularProgressIndicator()
            }

        } else{

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {viewModel.getTeamsStandings()}
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    Spacer(Modifier.size(5.dp))

                    BannerAdView("ca-app-pub-3940256099942544/6300978111")

                    Spacer(Modifier.size(5.dp))

                    Text(
                        modifier = Modifier.align(Alignment.Start),
                        text = stringResource(R.string.teamsStandings),
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                    )

                    Spacer(Modifier.size(50.dp))

                    TeamsStandingsTable(
                        navControler,
                        list = teamsList,
                        errorMsg = R.string.conection
                    )

                    Spacer(Modifier.size(25.dp))

                    BannerAdView("ca-app-pub-3940256099942544/6300978111")

                }

            }

        }
    }

}

@Preview(showSystemUi = true)
@Composable
private fun TeamsTableScreenPreview() {
    TeamsScreen(rememberNavController())
}