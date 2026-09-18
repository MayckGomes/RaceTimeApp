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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import mayckgomes.com.racetimeapp.components.home.*

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
        .padding(12.dp)) {

        // Header
        Text(text = "RACE TIME", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(12.dp))

        // Next Race block
        when (val s = vm.nextRace.collectAsState().value) {
            is BlockState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(180.dp)) { Text("Loading next race...") }
            is BlockState.Error -> Text("Error: ${s.message}")
            is BlockState.Success -> {
                NextRaceCard(
                    raceName = s.data.raceName,
                                circuitName = s.data.raceName,
                    round = s.data.round,
                    length = null,
                    laps = null,
                    lapRecord = null,
                    countdown = vm.countdownSeconds.collectAsState().value,
                    isStale = s.isStale
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last Race
        when (val s = vm.lastRace.collectAsState().value) {
            is BlockState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(160.dp)) { Text("Loading last race...") }
            is BlockState.Error -> Text("Error: ${s.message}")
            is BlockState.Success -> LastRaceCard(s.data, s.isStale)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Standings
        val drivers = when (val s = vm.driversTop.collectAsState().value) {
            is BlockState.Success -> s.data.map { it.Driver.givenName + " " + it.Driver.familyName }
            else -> listOf("—","—","—")
        }
        val constructors = when (val s = vm.constructorsTop.collectAsState().value) {
            is BlockState.Success -> s.data.map { it.Constructor.name }
            else -> listOf("—","—","—")
        }

        StandingsCards(drivers = drivers, constructors = constructors)

        Spacer(modifier = Modifier.height(12.dp))

        // Current session (placeholder)
        CurrentSessionBar(sessionName = "FREE PRACTICE 3", circuitName = "MONZA", elapsed = "32:17")

        Spacer(modifier = Modifier.height(24.dp))

        // Refresh action
        Button(onClick = { vm.refreshAll(isOnline = true) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Refresh")
        }
    }
}

