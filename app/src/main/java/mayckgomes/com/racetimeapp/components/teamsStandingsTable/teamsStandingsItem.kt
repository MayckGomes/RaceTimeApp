package mayckgomes.com.racetimeapp.components.teamsStandingsTable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.domain.models.ConstructorStandings

@Composable
fun TeamsStandingsItem(team: ConstructorStandings) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(1f)
            .height(35.dp)
    ) {

        Spacer(Modifier.size(5.dp))

        Text(
            text = team.position,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.25f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = team.Constructor.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.5f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = team.points,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}