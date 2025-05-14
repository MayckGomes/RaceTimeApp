package mayckgomes.com.racetimeapp.presentation.teams

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.teamsStandingsTable.TeamsStandingsTable
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen() {

    val viewModel = viewModel<teamsViewModel>()

    val teamsList = viewModel.teamsList.collectAsStateWithLifecycle().value

    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    RaceTimeAppTheme {

        Scaffold { padding ->

            if (isLoading){

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .padding(padding)
                ) {
                    CircularProgressIndicator()
                }

            } else{

                Log.d("teste", "TeamsScreen: $teamsList")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .systemBarsPadding()
                        .padding(padding)
                        .padding(10.dp)
                ) {

                    Text(
                        modifier = Modifier.align(Alignment.Start),
                        text = stringResource(R.string.teamsStandings),
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                    )

                    Spacer(Modifier.size(50.dp))

                    TeamsStandingsTable(teamsList)

                }
            }
        }
    }

}

@Preview(showSystemUi = true)
@Composable
private fun TeamsTableScreenPreview() {
    TeamsScreen()
}