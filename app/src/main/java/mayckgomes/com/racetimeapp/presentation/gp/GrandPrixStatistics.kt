@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package mayckgomes.com.racetimeapp.presentation.gp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.components.statistics.*
import mayckgomes.com.racetimeapp.domain.weekend.*

fun LazyListScope.grandPrixStatistics(
    state: GrandPrixState,
    season: Int,
    round: String,
    selectedType: String?,
    part: Int,
    driverId: String?,
    onType: (String) -> Unit,
    onPart: (Int) -> Unit,
    onSelectDriver: (String) -> Unit,
    onLoadPits: (Boolean) -> Unit,
    onLoadLaps: (String, Boolean) -> Unit,
    onDriver: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val results =
        state.results.values
            .flatMap { it.data }
            .filter {
                it.season == season.toString() &&
                    it.round == round &&
                    it.type in setOf(SessionType.RACE, SessionType.SPRINT) &&
                    it.entries.isNotEmpty()
            }
    val result =
        results.firstOrNull { it.type.name == selectedType }
            ?: results.firstOrNull { it.type == SessionType.RACE }
            ?: results.firstOrNull()
    item(key = "stats_selector") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            results
                .sortedBy { it.type.ordinal }
                .forEach { data ->
                    GrandPrixChoiceChip(
                        selected = result?.type == data.type,
                        onClick = { onType(data.type.name) },
                        label = stringResource(sessionLabel(data.type)),
                    )
                }
        }
        if (result == null)
            Text(
                stringResource(
                    when {
                        state.loading -> R.string.gp_loading_results
                        state.results
                            .filterKeys { it in setOf(SessionType.RACE, SessionType.SPRINT) }
                            .values
                            .any { it.failed } -> R.string.home_load_error
                        else -> R.string.stat_wait_result
                    }
                )
            )
        result?.let { data ->
            state.results[data.type]?.let { block ->
                DataNotice(block.stale, block.failed, block.updatedAt, onRefresh)
                if (!block.stale && !block.failed && block.updatedAt > 0)
                    Text(
                        stringResource(
                            R.string.v2_updated,
                            localDateTime(java.time.Instant.ofEpochMilli(block.updatedAt)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.stat_summary, R.string.stat_pit_stops, R.string.stat_laps)
                .forEachIndexed { index, title ->
                    GrandPrixChoiceChip(
                        selected = part == index,
                        onClick = { onPart(index) },
                        label = stringResource(title),
                    )
                }
        }
    }
    if (result == null) return
    val stats = raceStatistics(result) ?: return
    if (part == 0) {
        item(key = "highlights") { RaceHighlightsCard(stats, onDriver) }
        item(key = "finish_groups") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FinishCategory.entries.forEach { category ->
                    stats.groups[category]
                        ?.takeIf { it.isNotEmpty() }
                        ?.let {
                            Text(
                                stringResource(
                                    R.string.stat_group_count,
                                    stringResource(categoryLabel(category)),
                                    it.size,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                }
            }
            Text(
                stringResource(R.string.stat_status_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(
            result.entries.filter { it.driverId.isNotBlank() },
            key = { "stat_driver/${it.driverId}" },
        ) { entry ->
            DriverStatisticsRow(entry, stats.changes[entry.driverId]) { onDriver(entry.driverId) }
        }
        return
    }
    if (result.type != SessionType.RACE) {
        item(key = "race_only") { Text(stringResource(R.string.stat_race_only)) }
        return
    }
    if (part == 1) {
        val block = state.pitStops
        item(key = "pits_action") {
            Text(stringResource(R.string.stat_pit_note), style = MaterialTheme.typography.bodySmall)
            if (!supportsPitStops(season)) Text(stringResource(R.string.stat_pit_coverage))
            else {
                RaceActionButton(
                    label =
                        stringResource(
                            if (block == null) R.string.stat_load_pits else R.string.home_refresh
                        ),
                    onClick = { onLoadPits(block != null) },
                    enabled = block?.loading != true,
                )
                block?.let {
                    ExtraState(it.loading, it.failed, it.stale, it.updatedAt, it.data.isEmpty()) {
                        onLoadPits(true)
                    }
                }
            }
        }
        block?.data?.let { stops ->
            items(stops, key = { "pit/${it.driverId}/${it.stop}/${it.lap}/${it.time}" }) { stop ->
                RacingCard(accent = false) {
                    val driver = result.entries.firstOrNull { it.driverId == stop.driverId }
                    RaceActionButton(
                        label = driver?.name?.takeIf(String::isNotBlank) ?: stop.driverId,
                        onClick = { onDriver(stop.driverId) },
                    )
                    Text(
                        stringResource(
                            R.string.stat_stop_lap,
                            stop.stop.ifBlank { "—" },
                            stop.lap.ifBlank { "—" },
                        )
                    )
                    if (stop.duration.isNotBlank())
                        Text(
                            stringResource(R.string.stat_duration, stop.duration),
                            style = MaterialTheme.typography.titleMedium,
                        )
                }
            }
        }
        return
    }
    val selectable = result.entries.filter { it.driverId.matches(Regex("[a-zA-Z0-9_]+")) }
    val selected = selectable.firstOrNull { it.driverId == driverId } ?: selectable.firstOrNull()
    val block = selected?.let { state.lapTimes[it.driverId] }
    item(key = "laps_action") {
        Text(stringResource(R.string.stat_lap_note), style = MaterialTheme.typography.bodySmall)
        if (!supportsLapTimes(season)) Text(stringResource(R.string.stat_lap_coverage))
        else if (selected != null) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                RaceActionButton(
                    label =
                        stringResource(
                            R.string.stat_choose_driver,
                            selected.name.ifBlank { selected.driverId },
                        ),
                    onClick = { expanded = true },
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    selectable.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.name.ifBlank { entry.driverId }) },
                            onClick = {
                                expanded = false
                                onSelectDriver(entry.driverId)
                            },
                        )
                    }
                }
            }
            RaceActionButton(
                label =
                    stringResource(
                        if (block == null) R.string.stat_load_laps else R.string.home_refresh
                    ),
                onClick = { onLoadLaps(selected.driverId, block != null) },
                enabled = block?.loading != true,
            )
            block?.let {
                ExtraState(it.loading, it.failed, it.stale, it.updatedAt, it.data.isEmpty()) {
                    onLoadLaps(selected.driverId, true)
                }
            }
        }
    }
    block?.data?.let { laps ->
        items(laps, key = { "lap/${it.driverId}/${it.lap}" }) { lap ->
            RacingCard(accent = false) {
                Text(
                    stringResource(R.string.stat_lap, lap.lap),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (lap.time.isNotBlank())
                    Text(lap.time, style = MaterialTheme.typography.headlineMedium)
                if (lap.position.isNotBlank())
                    Text(
                        stringResource(R.string.stat_lap_position, lap.position),
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
    }
}

@Composable
private fun ExtraState(
    loading: Boolean,
    failed: Boolean,
    stale: Boolean,
    updatedAt: Long,
    empty: Boolean,
    onRetry: () -> Unit,
) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    DataNotice(stale, failed, updatedAt, onRetry)
    if (empty && !loading && !failed) Text(stringResource(R.string.stat_no_extra))
    if (updatedAt > 0 && !stale && !failed)
        Text(
            stringResource(
                R.string.v2_updated,
                localDateTime(java.time.Instant.ofEpochMilli(updatedAt)),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
}
