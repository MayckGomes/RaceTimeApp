package mayckgomes.com.racetimeapp.components.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.weekend.*

fun categoryLabel(category: FinishCategory): Int =
    when (category) {
        FinishCategory.FINISHED -> R.string.stat_finished
        FinishCategory.RETIRED -> R.string.stat_retired
        FinishCategory.DISQUALIFIED -> R.string.stat_disqualified
        FinishCategory.NOT_STARTED -> R.string.stat_not_started
        FinishCategory.OTHER -> R.string.stat_other
    }

@Composable
fun RaceHighlightsCard(stats: RaceStatistics, onDriver: (String) -> Unit) {
    RacingCard {
        SectionLabel(stringResource(R.string.stat_highlights), true)
        stats.podium
            .firstOrNull { officialPosition(it) == 1 }
            ?.let {
                Text(
                    stringResource(R.string.stat_winner),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(onClick = { onDriver(it.driverId) }) {
                    Text(
                        it.name.ifBlank { it.driverId },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        if (stats.podium.isNotEmpty()) {
            SectionLabel(stringResource(R.string.stat_podium))
            stats.podium.forEach {
                Text(
                    "${it.position} · ${it.name.ifBlank { it.driverId }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        stats.fastest?.let {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionLabel(stringResource(R.string.stat_fastest))
            Text(it.name.ifBlank { it.driverId }, style = MaterialTheme.typography.titleMedium)
            Text(it.fastestTime, style = MaterialTheme.typography.headlineMedium)
            it.fastestLap
                .toIntOrNull()
                ?.takeIf { lap -> lap > 0 }
                ?.let { lap ->
                    Text(
                        stringResource(R.string.stat_lap, lap),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
        }
        if (stats.mostGained.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionLabel(stringResource(R.string.stat_most_gained))
            stats.mostGained.forEach {
                Text(
                    stringResource(
                        R.string.stat_gain_driver,
                        it.entry.name.ifBlank { it.entry.driverId },
                        it.delta,
                    )
                )
            }
        }
        Text(
            stringResource(R.string.stat_calculation_note),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun DriverStatisticsRow(entry: ResultEntry, delta: Int?, onDriver: () -> Unit) {
    RacingCard(accent = false) {
        TextButton(onClick = onDriver, contentPadding = PaddingValues(0.dp)) {
            Text(
                entry.name.ifBlank { entry.driverId },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (entry.team.isNotBlank()) Text(entry.team, style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(
                R.string.stat_grid_final,
                entry.grid.toIntOrNull()?.takeIf { it > 0 }?.toString() ?: "—",
                entry.position.ifBlank { entry.positionText.ifBlank { "—" } },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (entry.points.isNotBlank())
            Text(
                stringResource(R.string.v2_points, entry.points),
                style = MaterialTheme.typography.labelLarge,
            )
        entry.completedLaps
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?.let {
                Text(
                    stringResource(R.string.stat_laps_completed, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        if (delta != null)
            Text(
                stringResource(
                    when {
                        delta > 0 -> R.string.stat_gained
                        delta < 0 -> R.string.stat_lost
                        else -> R.string.stat_unchanged
                    },
                    kotlin.math.abs(delta),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        Text(
            stringResource(categoryLabel(finishCategory(entry))),
            style = MaterialTheme.typography.labelLarge,
        )
        if (entry.status.isNotBlank())
            Text(entry.status, style = MaterialTheme.typography.bodySmall)
        if (timingMillis(entry.fastestTime) != null)
            Text(
                stringResource(R.string.stat_personal_fastest, entry.fastestTime),
                style = MaterialTheme.typography.bodySmall,
            )
    }
}
