package mayckgomes.com.racetimeapp.components.driversQualiResultTable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import mayckgomes.com.racetimeapp.components.driversQualiResultTable.DriversQualiResultItem
import mayckgomes.com.racetimeapp.domain.models.LastDriverPosition
import mayckgomes.com.racetimeapp.domain.models.QualiDriverPosition
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@Composable
fun DriversQualiTable(navController: NavController,list: List<QualiDriverPosition>) {
    RaceTimeAppTheme {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 700.dp),
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
                        modifier = Modifier.weight(0.32f)
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
                }

                if (list.isNotEmpty()){
                    LazyColumn(
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        items(list) {
                            DriversQualiResultItem(navController, it)
                        }
                    }
                } else {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {

                        Text(
                            text = stringResource(R.string.conection)
                        )

                    }

                }

            }
        }
    }
}
