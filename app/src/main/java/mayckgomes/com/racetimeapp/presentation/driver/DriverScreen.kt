@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package mayckgomes.com.racetimeapp.presentation.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.awaitCancellation
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.navgation.*
import mayckgomes.com.racetimeapp.ui.theme.constructorColor
import org.koin.androidx.compose.koinViewModel

@Composable
fun DriverScreen(navController: NavController, id: String) {
    val vm: DriverDetailsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val racing by vm.racing.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val isFavorite = id in favorites.drivers
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(id, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.load(id)
            awaitCancellation()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.driver_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.v2_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.toggleFavorite(id) },
                        enabled = !state.savingFavorite && id.matches(Regex("[a-zA-Z0-9_]+")),
                    ) {
                        Icon(
                            if (isFavorite) Icons.Outlined.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription =
                                stringResource(
                                    if (isFavorite) R.string.fav_remove else R.string.fav_add
                                ),
                            tint =
                                if (isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.load(id, true) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            DriverDetailsContent(
                state,
                racing.year,
                racing.championship,
                favorites.drivers[id].orEmpty(),
                vm.clock,
                onRefresh = { vm.load(id, true) },
                onTeam = { navController.navigate(teamRoute(it)) },
                onRace = { result ->
                    navController.navigate(
                        grandPrixRoute(result.season.toInt(), result.round, SessionType.RACE.name)
                    )
                },
            )
        }
    }
}

@Composable
fun DriverDetailsContent(
    state: DriverDetailsState,
    year: Int,
    championship: DataBlock<ChampionshipState>,
    savedName: String,
    clock: Clock,
    onRefresh: () -> Unit,
    onTeam: (String) -> Unit,
    onRace: (PublishedResult) -> Unit,
) {
    val standing = championship.data.drivers.singleOrNull { it.id == state.id }
    val info = championship.data.driversInfo
    val profile = state.profile.data
    val name =
        profile?.name?.takeIf(String::isNotBlank)
            ?: standing?.name?.takeIf(String::isNotBlank)
            ?: savedName.ifBlank { state.id }
    val color =
        constructorColor(standing?.constructorId, MaterialTheme.colorScheme.onSurfaceVariant)
    val number = profile?.number?.takeIf(String::isNotBlank) ?: standing?.number.orEmpty()
    val locale = LocalContext.current.resources.configuration.locales[0]
    val localClock = clock.withZone(ZoneId.systemDefault())
    val birth = profile?.let { driverBirthDate(it.dateOfBirth, localClock) }
    var section by rememberSaveable(state.id) { mutableIntStateOf(0) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "identity") {
            RacingCard(accent = false) {
                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp)) {
                    Canvas(Modifier.matchParentSize()) {
                        val x = size.width * .72f
                        drawLine(
                            color.copy(alpha = .16f),
                            Offset(x, 0f),
                            Offset(x - size.height * .45f, size.height),
                            16.dp.toPx(),
                        )
                        drawLine(
                            color.copy(alpha = .08f),
                            Offset(x + 24.dp.toPx(), 0f),
                            Offset(x - size.height * .45f + 24.dp.toPx(), size.height),
                            2.dp.toPx(),
                        )
                    }
                    if (number.isNotBlank())
                        Text(
                            number,
                            modifier = Modifier.align(Alignment.CenterEnd).clearAndSetSemantics {},
                            fontSize = 120.sp,
                            color = color.copy(alpha = .09f),
                            fontFamily = mayckgomes.com.racetimeapp.ui.theme.BarlowCondensed,
                        )
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            RacingMark()
                            SectionLabel(stringResource(R.string.driver))
                        }
                        Text(
                            name.ifBlank { stringResource(R.string.driver_profile_title) },
                            style = MaterialTheme.typography.displayLarge,
                        )
                        if (number.isNotBlank())
                            Text(
                                "#$number",
                                style = MaterialTheme.typography.headlineMedium,
                                color = color,
                            )
                        profile?.code?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.labelLarge)
                        }
                        profile?.nationality?.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state.loading && profile == null)
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
                DataNotice(
                    state.profile.stale,
                    state.profile.failed,
                    state.profile.updatedAt,
                    onRefresh,
                )
                if (profile == null && !state.loading && !state.profile.failed)
                    Text(
                        stringResource(R.string.driver_profile_missing),
                        style = MaterialTheme.typography.bodySmall,
                    )
                if (state.favoriteFailed)
                    Text(
                        stringResource(R.string.fav_save_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        }
        item(key = "sections") {
            ScrollableTabRow(
                selectedTabIndex = section,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                listOf(R.string.driver_overview, R.string.fav_latest_race, R.string.v2_about)
                    .forEachIndexed { index, title ->
                        Tab(
                            selected = section == index,
                            onClick = { section = index },
                            text = { Text(stringResource(title)) },
                        )
                    }
            }
        }
        if (section == 0)
            item(key = "championship") {
                RacingCard(accent = false) {
                    SectionLabel(stringResource(R.string.driver_championship, year))
                    if (standing != null) {
                        if (standing.team.isNotBlank())
                            Text(
                                standing.team,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            DriverMetric(stringResource(R.string.position), standing.position)
                            DriverMetric(stringResource(R.string.points), standing.points)
                            DriverMetric(stringResource(R.string.driver_wins), standing.wins)
                        }
                        if (standing.constructorId.isNotBlank())
                            TextButton(onClick = { onTeam(standing.constructorId) }) {
                                Text(stringResource(R.string.driver_view_team))
                            }
                    } else
                        Text(
                            stringResource(
                                when {
                                    championship.loading -> R.string.fav_loading_standings
                                    info.failed -> R.string.fav_standings_unavailable
                                    else -> R.string.fav_not_ranked
                                },
                                year,
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    DataNotice(info.stale, info.failed, info.updatedAt, onRefresh)
                }
            }
        if (section == 2)
            item(key = "personal") {
                RacingCard(accent = false) {
                    SectionLabel(stringResource(R.string.driver_personal_info))
                    if (birth != null) {
                        Text(
                            stringResource(
                                R.string.driver_birth,
                                birth.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                        .withLocale(locale)
                                ),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        driverAge(profile?.dateOfBirth.orEmpty(), localClock)?.let {
                            Text(
                                stringResource(R.string.driver_age, it),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (number.isNotBlank())
                        Text(
                            stringResource(R.string.champ_number, number),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    if (profile == null && !state.loading)
                        Text(
                            stringResource(R.string.driver_profile_missing),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    profile?.nationality?.takeIf(String::isNotBlank)?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        if (section == 1)
            item(key = "recent") {
                RacingCard(accent = false) {
                    SectionLabel(stringResource(R.string.fav_latest_race))
                    val result = state.recent.data
                    val entry = result?.let { favoriteBestEntry(it, state.id, false) }
                    if (result != null && entry != null) {
                        Text(
                            result.raceName,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            listOf(result.season, localDate(result.date)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(
                                R.string.fav_race_position,
                                entry.position.ifBlank { entry.positionText.ifBlank { "—" } },
                            ),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (entry.team.isNotBlank())
                            Text(entry.team, style = MaterialTheme.typography.bodySmall)
                        if (entry.points.isNotBlank())
                            Text(
                                stringResource(R.string.v2_points, entry.points),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        if (entry.status.isNotBlank())
                            Text(entry.status, style = MaterialTheme.typography.bodySmall)
                        if (entry.time.isNotBlank())
                            Text(entry.time, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onRace(result) }) {
                            Text(stringResource(R.string.v2_full_result))
                        }
                    } else if (state.loading)
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                    else
                        Text(
                            stringResource(
                                if (state.recent.failed) R.string.fav_result_unavailable
                                else R.string.fav_no_recent_result
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    DataNotice(
                        state.recent.stale,
                        state.recent.failed,
                        state.recent.updatedAt,
                        onRefresh,
                    )
                }
            }
        item(key = "source") {
            Text(
                stringResource(R.string.v2_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DriverMetric(label: String, value: String) {
    if (value.isBlank()) return
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            Modifier.widthIn(min = 80.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(value, style = MaterialTheme.typography.displayLarge)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
