package mayckgomes.com.racetimeapp.components.driversResultTable

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
import androidx.compose.ui.unit.dp

@Composable
fun DriversResultItem(driver: Driver) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(1f)
            .height(35.dp)
    ) {
        Text(
            text = driver.position,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(0.5f)
        )

        Spacer(Modifier.size(5.dp))

        VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.size(5.dp))

        Text(
            text = driver.name,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.5f)
        )

        Spacer(Modifier.size(5.dp))

        VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.size(5.dp))

        Text(
            text = driver.team,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.size(5.dp))

        VerticalDivider(color = MaterialTheme.colorScheme.onBackground)

        Spacer(Modifier.size(5.dp))

        Text(
            text = driver.status,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}