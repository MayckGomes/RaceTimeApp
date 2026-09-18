package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeOverview() {
    val vm: HomeViewModel = koinViewModel()

    // Start refresh on first composition
    DisposableEffect(Unit) {
        vm.refreshAll(isOnline = true)
        onDispose { vm.stopCountdown() }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(8.dp)) {

        // Header
        Text(text = "Season", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(8.dp))

        // Next Race block
        when (val s = vm.nextRace.collectAsState().value) {
            is BlockState.Loading -> Text("Loading next race...")
            is BlockState.Error -> Text("Error: ${s.message}")
            is BlockState.Success -> NextRaceCard(s.data, s.isStale, vm.countdownSeconds.collectAsState().value)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Last Race
        when (val s = vm.lastRace.collectAsState().value) {
            is BlockState.Loading -> Text("Loading last race...")
            is BlockState.Error -> Text("Error: ${s.message}")
            is BlockState.Success -> LastRaceCard(s.data, s.isStale)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drivers
        when (val s = vm.driversTop.collectAsState().value) {
            is BlockState.Loading -> Text("Loading drivers...")
            is BlockState.Error -> Text("Error: ${s.message}")
            is BlockState.Success -> StandingsPreviewCard("Drivers", s.data.map { it.Driver.givenName + " " + it.Driver.familyName })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Constructors
        when (val s = vm.constructorsTop.collectAsState().value) {
            is BlockState.Loading -> Text("Loading constructors...")
            is BlockState.Error -> Text("Error: ${s.message}")
            is BlockState.Success -> StandingsPreviewCard("Constructors", s.data.map { it.Constructor.name })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { vm.refreshAll(isOnline = true) }) {
            Text("Refresh")
        }
    }
}

@Composable
private fun NextRaceCard(race: mayckgomes.com.racetimeapp.domain.models.RacesCalendar, isStale: Boolean, countdownSeconds: Long?) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = "Next: ${race.raceName}", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Round ${race.round}")
        if (countdownSeconds != null) {
            val days = countdownSeconds / 86400
            val hours = (countdownSeconds % 86400) / 3600
            val mins = (countdownSeconds % 3600) / 60
            val secs = countdownSeconds % 60
            Text(text = "Starts in: ${days}d ${hours}h ${mins}m ${secs}s")
        }
        if (isStale) Text("Data desatualizada", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LastRaceCard(race: mayckgomes.com.racetimeapp.domain.models.Circuits, isStale: Boolean) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = "Last: ${race.raceName}", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Circuit: ${race.Circuit.circuitName}")
        if (isStale) Text("Data desatualizada", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StandingsPreviewCard(title: String, entries: List<String>) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        entries.forEach { Text(it) }
    }
}
