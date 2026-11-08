@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.model.sessionInstant
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.ui.theme.constructorColor
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeOverview(
    onCalendar: () -> Unit = {},
    onDrivers: () -> Unit = {},
    onTeams: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onGrandPrix: (Int, String, SessionType?) -> Unit = { _, _, _ -> },
    onDriver: (String) -> Unit = {},
    onTeam: (String) -> Unit = {},
) {
    val vm: HomeViewModel = koinViewModel()
    val data by vm.racingData.collectAsStateWithLifecycle()
    val context by vm.weekendContext.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(vm.clock.instant()) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.refreshAll(force = false)
            while (true) {
                now = vm.clock.instant()
                delay(1000)
            }
        }
    }
    val snapshots =
        remember(data.results, data.calendar.data) {
            homeResults(data.calendar.data, data.results.values)
        }
    val latestResult = snapshots.firstOrNull()?.result
    val latestWeekend =
        latestResult?.let { result ->
            data.calendar.data.firstOrNull {
                it.season == result.season && it.round == result.round
            }
        }
    val sprintResult =
        snapshots.firstOrNull {
            it.result.type == SessionType.SPRINT &&
                it.result.season == latestResult?.season &&
                it.result.round == latestResult?.round
        }
    val hasSprint =
        latestWeekend?.sessions()?.any { it.type == SessionType.SPRINT } ?: (sprintResult != null)
    val selectableResults =
        snapshots.filter {
            it.result.type != SessionType.SPRINT || (hasSprint && it == sprintResult)
        }
    val availableTypes =
        selectableResults
            .map { it.result.type }
            .distinct()
            .let { types ->
                if (hasSprint && SessionType.SPRINT !in types) types + SessionType.SPRINT else types
            }
    val selected =
        selectableResults.firstOrNull { it.result.type.name == selectedType }
            ?: selectableResults.firstOrNull()
    val standings = data.championship
    val calendar = data.calendar
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.refreshAll() }) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "weekend") {
                when {
                    calendar.loading && (calendar.data.isEmpty() || context.weekend == null) ->
                        HomeCardSkeleton(HomeSkeletonKind.WEEKEND)
                    context.weekend != null ->
                        WeekendHeroCard(context, now, calendar.stale) {
                            context.weekend?.let {
                                onGrandPrix(it.season.toIntOrNull() ?: data.year, it.round, null)
                            }
                        }
                    else ->
                        RacingCard {
                            SectionLabel(stringResource(weekendLabel(context.state)), true)
                            Text(
                                stringResource(
                                    if (calendar.failed) R.string.home_load_error
                                    else R.string.v2_no_calendar
                                )
                            )
                        }
                }
                DataNotice(calendar.stale, calendar.failed, calendar.updatedAt, { vm.refreshAll() })
            }
            item(key = "result") {
                when {
                    selected != null ->
                        DashboardResultCard(
                            selected,
                            resultSessionDate(data.calendar.data, selected.result),
                            availableTypes,
                            onSelect = { type ->
                                if (
                                    type == SessionType.SPRINT &&
                                        sprintResult == null &&
                                        latestWeekend != null
                                ) {
                                    onGrandPrix(
                                        latestWeekend.season.toInt(),
                                        latestWeekend.round,
                                        type,
                                    )
                                } else {
                                    selectedType = type.name
                                }
                            },
                            onRetry = { vm.refreshAll() },
                            onOpen = {
                                onGrandPrix(
                                    selected.result.season.toInt(),
                                    selected.result.round,
                                    selected.result.type,
                                )
                            },
                        )
                    (refreshing || calendar.loading) -> HomeCardSkeleton(HomeSkeletonKind.RESULT)
                    else ->
                        RacingCard {
                            SectionLabel(stringResource(R.string.v2_latest_result))
                            Text(
                                stringResource(
                                    if (data.results.values.any { it.failed })
                                        R.string.home_load_error
                                    else R.string.v2_no_published_result
                                )
                            )
                            if (data.results.values.any { it.failed })
                                RaceActionButton(
                                    stringResource(R.string.home_retry),
                                    { vm.refreshAll() },
                                )
                        }
                }
            }
            item(key = "championship") {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val drivers: @Composable () -> Unit = {
                        if (
                            standings.loading &&
                                standings.data.drivers.isEmpty() &&
                                standings.data.driversInfo.updatedAt == 0L
                        )
                            HomeCardSkeleton(HomeSkeletonKind.STANDINGS)
                        else
                            StandingsCard(
                                stringResource(R.string.home_drivers),
                                standings.data.drivers.take(3).map {
                                    StandingRow(
                                        it.position.ifBlank { "—" },
                                        it.name,
                                        it.team,
                                        it.points.ifBlank { "—" },
                                        constructorColor(it.constructorId),
                                    )
                                },
                                standings.stale,
                                onDrivers,
                            )
                    }
                    val teams: @Composable () -> Unit = {
                        if (
                            standings.loading &&
                                standings.data.constructors.isEmpty() &&
                                standings.data.constructorsInfo.updatedAt == 0L
                        )
                            HomeCardSkeleton(HomeSkeletonKind.STANDINGS)
                        else
                            StandingsCard(
                                stringResource(R.string.home_teams),
                                standings.data.constructors.take(3).map {
                                    StandingRow(
                                        it.position.ifBlank { "—" },
                                        it.name,
                                        "",
                                        it.points.ifBlank { "—" },
                                        constructorColor(it.id),
                                    )
                                },
                                standings.stale,
                                onTeams,
                            )
                    }
                    if (maxWidth >= 600.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) { drivers() }
                            Box(Modifier.weight(1f)) { teams() }
                        }
                    else
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            drivers()
                            teams()
                        }
                }
                DataNotice(
                    standings.stale,
                    standings.failed,
                    standings.updatedAt,
                    { vm.refreshAll() },
                )
            }
            if (shouldShowNextRace(context))
                item(key = "next_race") {
                    context.nextRace?.let { race ->
                        NextRaceCard(
                            race,
                            calendar.data.maxOfOrNull { it.round.toIntOrNull() ?: 0 }
                                ?: calendar.data.size,
                            secondsUntil(sessionInstant(race.date, race.time), now),
                            calendar.stale,
                        )
                        RaceActionButton(
                            stringResource(R.string.v2_gp_details),
                            {
                                onGrandPrix(
                                    race.season.toIntOrNull() ?: data.year,
                                    race.round,
                                    null,
                                )
                            },
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            item(key = "favorites") {
                RacingCard(accent = false) {
                    SectionLabel(stringResource(R.string.myfavorites))
                    if (favorites.drivers.isEmpty() && favorites.constructors.isEmpty()) {
                        Text(
                            stringResource(R.string.v2_favorites_empty),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        RaceActionButton(
                            stringResource(R.string.v2_choose_favorites),
                            onDrivers,
                            Modifier.padding(top = 12.dp),
                        )
                    } else {
                        favorites.drivers.entries.take(3).forEach { (id, label) ->
                            val standing = standings.data.drivers.firstOrNull { it.id == id }
                            val last =
                                snapshots.firstNotNullOfOrNull { snapshot ->
                                    snapshot.result.entries
                                        .firstOrNull { it.driverId == id }
                                        ?.let { snapshot.result to it }
                                }
                            val lines =
                                listOfNotNull(
                                    standing?.let {
                                        stringResource(
                                            R.string.v2_favorite_standing,
                                            it.position.ifBlank { "—" },
                                            it.points.ifBlank { "—" },
                                        )
                                    },
                                    last?.let {
                                        stringResource(
                                            R.string.v2_favorite_result,
                                            stringResource(sessionLabel(it.first.type)),
                                            it.second.position.ifBlank {
                                                it.second.positionText.ifBlank { "—" }
                                            },
                                        )
                                    },
                                )
                            HomeFavoriteRow(standing?.name ?: label.ifBlank { id }, lines) {
                                onDriver(id)
                            }
                        }
                        favorites.constructors.entries.take(2).forEach { (id, label) ->
                            val standing = standings.data.constructors.firstOrNull { it.id == id }
                            val lines =
                                listOfNotNull(
                                    standing?.let {
                                        stringResource(
                                            R.string.v2_favorite_standing,
                                            it.position.ifBlank { "—" },
                                            it.points.ifBlank { "—" },
                                        )
                                    }
                                )
                            HomeFavoriteRow(standing?.name ?: label.ifBlank { id }, lines) {
                                onTeam(id)
                            }
                        }
                        RaceActionButton(
                            stringResource(R.string.home_view_all),
                            onFavorites,
                            Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
            item(key = "calendar_action") {
                RaceActionButton(
                    stringResource(R.string.v2_open_calendar),
                    onCalendar,
                    Modifier.fillMaxWidth(),
                    primary = true,
                )
            }
        }
    }
}
