package mayckgomes.com.racetimeapp.components.teamsStandingsTable

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@Composable
fun TeamsStandingsTable(
    navController: NavController,
    list: List<ConstructorStandings>,
    errorMsg: Int = R.string.nofavteam
) {

    Log.d("teste", "TeamsStandingsTable: $list")

    RaceTimeAppTheme {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp, max = 500.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {

            Column(

            ) {
                // cabeçario
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth(1f)
                        .height(35.dp)
                ) {
                    Text(
                        text = stringResource(R.string.position),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(0.25f)
                    )

                    Spacer(Modifier.size(5.dp))

                    VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

                    Spacer(Modifier.size(5.dp))

                    Text(
                        text = stringResource(R.string.team),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1.5f)
                    )

                    Spacer(Modifier.size(5.dp))

                    VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

                    Spacer(Modifier.size(5.dp))

                    Text(
                        text = stringResource(R.string.points),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }


                if (list.isNotEmpty()){
                    LazyColumn {
                        items(list) {
                            TeamsStandingsItem(navController, it)
                        }
                    }
                } else {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    ) {

                        Text(
                            text = stringResource(errorMsg)
                        )

                    }

                }

            }
        }
    }
}