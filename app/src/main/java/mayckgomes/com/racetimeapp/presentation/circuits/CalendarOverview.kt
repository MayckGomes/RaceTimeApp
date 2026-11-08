@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.circuits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarOverview(onGrandPrix: (Int, String) -> Unit, showTitle: Boolean = true) {
    val vm: CalendarViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context by vm.context.collectAsStateWithLifecycle()
    var selectedYear by rememberSaveable { mutableIntStateOf(vm.activeYear) }
    var menu by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(selectedYear, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.load(selectedYear)
            awaitCancellation()
        }
    }
    val holder = rememberSaveableStateHolder()
    val calendar =
        if (state.year == selectedYear) state.calendar
        else
            state.calendar.copy(
                data = emptyList(),
                loading = true,
                failed = false,
                stale = false,
                updatedAt = 0,
            )
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showTitle)
                Text(
                    stringResource(R.string.home_calendar),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                )
            else Spacer(Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(selectedYear.toString()) }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    state.seasons.data.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                selectedYear = year
                                menu = false
                            },
                        )
                    }
                    if (vm.activeYear !in state.seasons.data)
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.gp_current_year, vm.activeYear))
                            },
                            onClick = {
                                selectedYear = vm.activeYear
                                menu = false
                            },
                        )
                    if (state.seasons.data.isEmpty())
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.gp_no_seasons)) },
                            onClick = {
                                vm.loadSeasons(true)
                                menu = false
                            },
                        )
                }
            }
        }
        holder.SaveableStateProvider(selectedYear) {
            PullToRefreshBox(
                isRefreshing = calendar.loading,
                onRefresh = {
                    vm.load(selectedYear, true)
                    vm.loadSeasons(true)
                },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "status") {
                        Text(
                            stringResource(R.string.v2_season, selectedYear),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        DataNotice(calendar.stale, calendar.failed, calendar.updatedAt) {
                            vm.load(selectedYear, true)
                        }
                        if (state.seasons.failed)
                            DataNotice(state.seasons.stale, true, state.seasons.updatedAt) {
                                vm.loadSeasons(true)
                            }
                        if (calendar.loading && calendar.data.isEmpty())
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (!calendar.loading && calendar.data.isEmpty())
                            Text(
                                stringResource(
                                    if (calendar.failed) R.string.home_load_error
                                    else R.string.v2_no_calendar
                                )
                            )
                        TextButton(
                            onClick = {
                                vm.load(selectedYear, true)
                                vm.loadSeasons(true)
                            }
                        ) {
                            Text(stringResource(R.string.home_refresh))
                        }
                    }
                    items(
                        calendar.data.sortedBy { it.date },
                        key = { "${it.season}/${it.round}" },
                    ) { race ->
                        val status = calendarEventStatus(race, context, vm.clock.instant())
                        val label =
                            when (status) {
                                CalendarEventStatus.RESULT_PUBLISHED -> R.string.v2_completed
                                CalendarEventStatus.CURRENT -> R.string.v2_contextual_gp
                                CalendarEventStatus.NEXT -> R.string.gp_next_in_season
                                CalendarEventStatus.PAST_SCHEDULE -> R.string.v2_past_schedule
                                CalendarEventStatus.UPCOMING -> R.string.v2_upcoming
                                CalendarEventStatus.TIME_UNKNOWN -> R.string.home_time_unknown
                            }
                        Box(
                            Modifier.clickable {
                                onGrandPrix(race.season.toIntOrNull() ?: selectedYear, race.round)
                            }
                        ) {
                            RacingCard(
                                accent =
                                    status in
                                        setOf(CalendarEventStatus.CURRENT, CalendarEventStatus.NEXT)
                            ) {
                                SectionLabel(
                                    stringResource(label),
                                    status == CalendarEventStatus.CURRENT,
                                )
                                Text(race.raceName, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    stringResource(R.string.v2_round, race.round),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                race.Circuit?.let { circuit ->
                                    Text(
                                        circuit.circuitName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    circuit.Location?.let { location ->
                                        Text(
                                            listOf(location.locality, location.country)
                                                .filter(String::isNotBlank)
                                                .joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Text(
                                    race
                                        .sessions()
                                        .firstOrNull { it.type == SessionType.RACE }
                                        ?.start
                                        ?.let(::localDateTime)
                                        ?: stringResource(
                                            R.string.gp_date_utc,
                                            localDate(race.date),
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
