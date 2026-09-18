package mayckgomes.com.racetimeapp.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.domain.models.Circuits

@Composable
fun NextRaceCard(
    raceName: String,
    circuitName: String,
    round: String,
    length: String?,
    laps: String?,
    lapRecord: String?,
    countdown: Long?,
    isStale: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("NEXT RACE", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                Text(raceName, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(circuitName, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))

                // countdown
                countdown?.let {
                    val days = it / 86400
                    val hours = (it % 86400) / 3600
                    val mins = (it % 3600) / 60
                    val secs = it % 60
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeBox(days.toString().padStart(2,'0'), "DAYS")
                        Spacer(Modifier.width(8.dp))
                        TimeBox(hours.toString().padStart(2,'0'), "HOURS")
                        Spacer(Modifier.width(8.dp))
                        TimeBox(mins.toString().padStart(2,'0'), "MINUTES")
                        Spacer(Modifier.width(8.dp))
                        TimeBox(secs.toString().padStart(2,'0'), "SECONDS")
                    }
                }

                if (isStale) Text("Data desatualizada", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // right column: circuit trace + specs
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(120.dp)) {
                // placeholder trace
                Box(modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color.DarkGray, Color.Black)))) {
                    // optionally draw SVG later
                }
                Spacer(Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("ROUND", style = MaterialTheme.typography.labelLarge)
                    Text(round, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    length?.let { Text("Length: $it", style = MaterialTheme.typography.bodySmall) }
                    laps?.let { Text("Laps: $it", style = MaterialTheme.typography.bodySmall) }
                    lapRecord?.let { Text("Lap record: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
fun TimeBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LastRaceCard(circuit: Circuits, isStale: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // background image placeholder
            Image(painter = painterResource(id = R.drawable.ic_race_placeholder), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("LAST RACE", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                Text(circuit.raceName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(circuit.Circuit.circuitName, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                // podium preview simplified
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("1"); Text("VER"); Text("+0.000") }
                    Column { Text("2"); Text("NOR"); Text("+7.582") }
                    Column { Text("3"); Text("LEC"); Text("+11.421") }
                }
            }
        }
    }
}

@Composable
fun StandingsCards(drivers: List<String>, constructors: List<String>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("DRIVERS' STANDINGS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                drivers.forEachIndexed { idx, name ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${idx+1}  ")
                        Text(name)
                        Text("--- PTS")
                    }
                }
            }
        }

        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CONSTRUCTORS' STANDINGS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                constructors.forEachIndexed { idx, name ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${idx+1}  ")
                        Text(name)
                        Text("--- PTS")
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentSessionBar(sessionName: String, circuitName: String, elapsed: String) {
    Card(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("CURRENT SESSION", style = MaterialTheme.typography.labelLarge)
                Text(sessionName, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Time elapsed", style = MaterialTheme.typography.bodySmall)
                Text(elapsed, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
