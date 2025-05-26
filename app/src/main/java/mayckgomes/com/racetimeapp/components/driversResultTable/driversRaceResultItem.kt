package mayckgomes.com.racetimeapp.components.driversResultTable

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.domain.models.LastDriverPosition
import mayckgomes.com.racetimeapp.navgation.driverRoute

@Composable
fun DriversResultItem(navController: NavController, driverPosition: LastDriverPosition) {

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
            color =
                when(driverPosition.position){
                    "1" -> Color.Yellow
                    "2" -> Color.Gray
                    "3" -> Color(0xFFCE8946)
                    else -> MaterialTheme.colorScheme.primary
                },
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


        Spacer(Modifier.size(5.dp))

        Text(
            text = driverPosition.status,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(0.7f)
        )
    }
}