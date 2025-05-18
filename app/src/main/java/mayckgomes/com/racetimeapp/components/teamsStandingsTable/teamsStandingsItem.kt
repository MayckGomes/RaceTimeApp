package mayckgomes.com.racetimeapp.components.teamsStandingsTable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings
import mayckgomes.com.racetimeapp.navgation.teamRoute

@Composable
fun TeamsStandingsItem(navController: NavController, teamPosition: ConstructorStandings) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(1f)
            .clickable{

                navController.navigate(teamRoute(teamPosition.Constructor.constructorId))

            }
    ) {

        Spacer(Modifier.size(5.dp))

        Text(
            text = teamPosition.position,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.25f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = teamPosition.Constructor.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.5f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = teamPosition.points,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}