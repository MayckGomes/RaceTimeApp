package mayckgomes.com.racetimeapp.components.driversStandingsTable

import android.util.Log
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
import mayckgomes.com.racetimeapp.domain.models.DriverPosition

@Composable
fun DriversStandingsItem(driver: DriverPosition) {
    Log.d("teste", "DriversStandingsItem: $driver")
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(1f)
            .height(35.dp)
    ) {
        Text(
            text = driver.position,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.3f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = "${driver.Driver.givenName} ${driver.Driver.familyName}",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.3f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = driver.Constructors.first().name,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.size(5.dp))

        Text(
            text = driver.points,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(0.25f)
        )
    }
}