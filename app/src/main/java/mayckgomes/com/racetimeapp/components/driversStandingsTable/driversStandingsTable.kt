package mayckgomes.com.racetimeapp.components.driversStandingsTable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

data class Driver(
    val name:String,
    val team: String,
    val position: String,
    val points: String
)

val driverstandingslist = listOf(
    Driver("Russell","Mercedes", "1", "100"),
    Driver("Hamilton","Ferrari", "2", "80"),
    Driver("Norris","McLaren", "3", "70"),
    Driver("Verstappen","Redbull", "4", "60"),
)

@Composable
fun DriversStandingsTable(list: List<Driver>) {
    RaceTimeAppTheme {

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {

            Column(
                Modifier.fillMaxSize(1f)
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
                        modifier = Modifier.weight(0.5f)
                    )

                    Spacer(Modifier.size(5.dp))

                    VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

                    Spacer(Modifier.size(5.dp))

                    Text(
                        text = stringResource(R.string.driver),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1.5f)
                    )

                    Spacer(Modifier.size(5.dp))

                    VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

                    Spacer(Modifier.size(5.dp))

                    Text(
                        text = stringResource(R.string.team),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
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

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyColumn {
                    items(list) {
                        DriversStandingsItem(it)
                    }
                }

            }
        }
    }
}

@Preview
@Composable
private fun DriversTablePreview() {
    DriversStandingsTable(driverstandingslist)
}