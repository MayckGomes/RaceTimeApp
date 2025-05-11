package mayckgomes.com.racetimeapp.presentation.teams

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.driversStandingsTable.DriversStandingsTable
import mayckgomes.com.racetimeapp.components.driversStandingsTable.driverstandingslist
import mayckgomes.com.racetimeapp.components.teamsStandingsTable.TeamsStandingsTable
import mayckgomes.com.racetimeapp.components.teamsStandingsTable.teamstandingslist
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen() {

    RaceTimeAppTheme {

        Scaffold { padding ->

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize(1f)
                    .systemBarsPadding()
                    .padding(padding)
                    .padding(25.dp)
            ) {

                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = stringResource(R.string.teamsStandings),
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                )

                Spacer(Modifier.size(50.dp))

                TeamsStandingsTable(teamstandingslist)

            }

        }

    }

}

@Preview(showSystemUi = true)
@Composable
private fun TeamsTableScreenPreview() {
    TeamsScreen()
}