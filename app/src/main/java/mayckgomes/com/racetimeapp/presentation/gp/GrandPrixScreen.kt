@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package mayckgomes.com.racetimeapp.presentation.gp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.calendar.SessionScheduleItem
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun GrandPrixScreen(
    season: Int,
    round: String,
    initialType: String?,
    onBack: () -> Unit,
    onDriver: (String) -> Unit,
) {
    val vm: GrandPrixViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(if (initialType != null) 1 else 0) }
    var chosen by rememberSaveable { mutableStateOf(initialType) }
    var statsType by rememberSaveable(season, round) { mutableStateOf<String?>(null) }
    var statsPart by rememberSaveable(season, round) { mutableIntStateOf(0) }
    var lapDriver by rememberSaveable(season, round) { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(vm.clock.instant()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val request = SessionType.entries.firstOrNull { it.name == initialType && it.endpoint != null }
    LaunchedEffect(season, round, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.load(season, round, requestedType = request)
            while (true) {
                now = vm.clock.instant()
                delay(1000)
            }
        }
    }
    val race = state.race
    val published =
        state.results.values
            .flatMap { it.data }
            .filter { it.entries.isNotEmpty() }
            .map { it.key }
            .toSet()
    val resultTypes =
        supportedResultTypes(
            race,
            state.results
                .filterValues { it.data.any { result -> result.entries.isNotEmpty() } }
                .keys,
            state.scheduleAvailable,
        )
    val type = resultTypes.firstOrNull { it.name == chosen } ?: resultTypes.firstOrNull()
    val block = type?.let { state.results[it] }
    val result = block?.data?.firstOrNull { it.season == season.toString() && it.round == round }
    val session =
        if (state.scheduleAvailable) race?.sessions()?.firstOrNull { it.type == type } else null
    val resolver = remember(now) { WeekendResolver(Clock.fixed(now, ZoneOffset.UTC)) }
    val context = resolver.resolve(Season(season, listOfNotNull(race)), published)
    val holder = rememberSaveableStateHolder()
    val retry = { vm.load(season, round, force = true, requestedType = type ?: request) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.v2_gp_details)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.v2_back),
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) },
            ) {
                listOf(
                        R.string.v2_weekend,
                        R.string.v2_results,
                        R.string.v2_information,
                        R.string.stat_title,
                    )
                    .forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = { Text(stringResource(label)) },
                        )
                    }
            }
            holder.SaveableStateProvider(
                "$tab/${if (tab == 1) type?.name.orEmpty() else if (tab == 3) "$statsType/$statsPart/$lapDriver" else ""}"
            ) {
                PullToRefreshBox(
                    isRefreshing = state.loading,
                    onRefresh = retry,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = rememberLazyListState(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "header") {
                            Text(
                                race?.raceName ?: stringResource(R.string.v2_round, round),
                                style = MaterialTheme.typography.displayLarge,
                            )
                            Text(
                                listOf(
                                        stringResource(R.string.v2_season, season),
                                        stringResource(R.string.v2_round, round),
                                    )
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            DataNotice(state.stale, state.failed, state.updatedAt, retry)
                            if (state.updatedAt > 0 && !state.stale && !state.failed)
                                Text(
                                    stringResource(
                                        R.string.v2_updated,
                                        localDateTime(
                                            java.time.Instant.ofEpochMilli(state.updatedAt)
                                        ),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            if (!state.scheduleAvailable && !state.loading)
                                Text(
                                    stringResource(
                                        if (race == null && !state.failed) R.string.gp_not_found
                                        else R.string.gp_schedule_missing
                                    )
                                )
                            RaceActionButton(
                                label = stringResource(R.string.home_refresh),
                                onClick = retry,
                                enabled = !state.loading,
                            )
                        }
                        when (tab) {
                            3 ->
                                grandPrixStatistics(
                                    state,
                                    season,
                                    round,
                                    statsType,
                                    statsPart,
                                    lapDriver,
                                    onType = { statsType = it },
                                    onPart = { statsPart = it },
                                    onSelectDriver = { lapDriver = it },
                                    onLoadPits = { vm.loadPitStops(season, round, it) },
                                    onLoadLaps = { driver, force ->
                                        lapDriver = driver
                                        vm.loadLapTimes(season, round, driver, force)
                                    },
                                    onDriver = onDriver,
                                    onRefresh = retry,
                                )
                            0 -> {
                                item(key = "zone") {
                                    Text(
                                        stringResource(R.string.home_local_time),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (state.scheduleAvailable)
                                    race?.sessions()?.let { sessions ->
                                        items(sessions, key = { it.key }) { item ->
                                            val available = item.key in published
                                            SessionScheduleItem(
                                                item,
                                                resolver.status(item, context),
                                                if (available)
                                                    ({
                                                        chosen = item.type.name
                                                        tab = 1
                                                    })
                                                else null,
                                            )
                                        }
                                    }
                            }
                            1 -> {
                                item(key = "selector") {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        resultTypes.forEach { candidate ->
                                            GrandPrixChoiceChip(
                                                label = stringResource(sessionLabel(candidate)),
                                                selected = type == candidate,
                                                onClick = { chosen = candidate.name },
                                            )
                                        }
                                    }
                                    block?.let {
                                        DataNotice(it.stale, it.failed, it.updatedAt, retry)
                                    }
                                    if (result?.type in setOf(SessionType.RACE, SessionType.SPRINT))
                                        RaceActionButton(
                                            label = stringResource(R.string.stat_title),
                                            onClick = {
                                                statsType = result?.type?.name
                                                statsPart = 0
                                                tab = 3
                                            },
                                        )
                                    if (type == null)
                                        Text(
                                            stringResource(
                                                when {
                                                    state.loading -> R.string.gp_loading_results
                                                    state.results.values.any { it.failed } ->
                                                        R.string.home_load_error
                                                    else -> R.string.v2_no_published_result
                                                }
                                            )
                                        )
                                    else {
                                        val availability =
                                            resultAvailability(
                                                type,
                                                session,
                                                block,
                                                type in state.resultLoading,
                                                state.scheduleAvailable,
                                                now,
                                            )
                                        if (availability == ResultAvailability.LOADING)
                                            LinearProgressIndicator(Modifier.fillMaxWidth())
                                        if (availability != ResultAvailability.AVAILABLE)
                                            Text(
                                                stringResource(
                                                    when (availability) {
                                                        ResultAvailability.REQUEST_FAILED ->
                                                            R.string.home_load_error
                                                        ResultAvailability.NOT_SCHEDULED ->
                                                            R.string.gp_not_scheduled
                                                        ResultAvailability.SCHEDULED ->
                                                            R.string.gp_future_session
                                                        ResultAvailability.UNKNOWN_TIME ->
                                                            R.string.home_time_unknown
                                                        ResultAvailability.UNSUPPORTED ->
                                                            R.string.v2_no_results_endpoint
                                                        ResultAvailability.NOT_CHECKED ->
                                                            R.string.gp_not_checked
                                                        ResultAvailability.LOADING ->
                                                            R.string.gp_loading_results
                                                        else -> R.string.v2_no_published_result
                                                    }
                                                )
                                            )
                                    }
                                }
                                result?.let { data ->
                                    items(data.entries, key = { it.driverId }) { row ->
                                        ResultEntryRow(
                                            row,
                                            data.type,
                                            onDriver = { onDriver(row.driverId) },
                                        )
                                    }
                                }
                            }
                            2 ->
                                item(key = "metadata") {
                                    RacingCard(accent = false) {
                                        SectionLabel(stringResource(R.string.v2_information))
                                        race?.Circuit?.let { circuit ->
                                            Text(
                                                circuit.circuitName,
                                                style = MaterialTheme.typography.headlineMedium,
                                            )
                                            circuit.Location?.let { location ->
                                                Text(
                                                    listOf(location.locality, location.country)
                                                        .filter(String::isNotBlank)
                                                        .joinToString(" · ")
                                                )
                                                if (
                                                    location.lat?.toDoubleOrNull()?.let {
                                                        it in -90.0..90.0
                                                    } == true &&
                                                        location.long?.toDoubleOrNull()?.let {
                                                            it in -180.0..180.0
                                                        } == true
                                                )
                                                    Text(
                                                        stringResource(
                                                            R.string.gp_coordinates,
                                                            location.lat.orEmpty(),
                                                            location.long.orEmpty(),
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                            }
                                            WikipediaAction(circuit.url, R.string.gp_circuit_link)
                                        }
                                        race?.let { data ->
                                            val start =
                                                data
                                                    .sessions()
                                                    .firstOrNull { it.type == SessionType.RACE }
                                                    ?.start
                                            Text(
                                                if (start != null)
                                                    stringResource(
                                                        R.string.v2_race_date,
                                                        localDateTime(start),
                                                    )
                                                else
                                                    stringResource(
                                                        R.string.gp_date_utc,
                                                        localDate(data.date),
                                                    )
                                            )
                                            if (state.scheduleAvailable) {
                                                val dates =
                                                    data
                                                        .sessions()
                                                        .map { it.date }
                                                        .filter(String::isNotBlank)
                                                        .sorted()
                                                if (dates.isNotEmpty())
                                                    Text(
                                                        stringResource(
                                                            R.string.gp_dates,
                                                            localDate(dates.first()),
                                                            localDate(dates.last()),
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                            }
                                            WikipediaAction(data.url, R.string.gp_event_link)
                                        }
                                        Text(
                                            stringResource(R.string.home_local_time),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            stringResource(R.string.v2_source),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 12.dp),
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WikipediaAction(value: String?, label: Int) {
    val url = wikipediaUrl(value) ?: return
    val context = LocalContext.current
    RaceActionButton(
        label = stringResource(label),
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                        context,
                        context.getString(R.string.gp_no_browser),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        },
    )
}

@Composable
internal fun GrandPrixChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = colors.surfaceVariant,
                labelColor = colors.onSurface,
                selectedContainerColor = colors.primaryContainer,
                selectedLabelColor = colors.onPrimaryContainer,
            ),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outline),
    )
}
