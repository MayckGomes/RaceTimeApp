package mayckgomes.com.racetimeapp.components.driversQualiResultTable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.domain.models.QualiDriverPosition
import mayckgomes.com.racetimeapp.navgation.driverRoute

@Composable
fun DriversQualiResultItem(navController: NavController, driverPosition: QualiDriverPosition) {

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(1f)
            .clickable {

                navController.navigate(driverRoute(driverPosition.Driver.driverId))

            }

    ) {
        Text(
            text = driverPosition.position,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.3f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = "${driverPosition.Driver.givenName} ${driverPosition.Driver.familyName}",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.5f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = driverPosition.Constructor.name,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}