@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.championship

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import mayckgomes.com.racetimeapp.components.ads.BannerAdView
import mayckgomes.com.racetimeapp.components.championship.StandingItem
import mayckgomes.com.racetimeapp.components.home.DataNotice
import mayckgomes.com.racetimeapp.components.home.localDateTime
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChampionshipScreen(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onDriver: (String) -> Unit,
    onTeam: (String) -> Unit,
) {
    val vm: ChampionshipViewModel = koinViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.refresh()
            awaitCancellation()
        }
    }
    val championship = data.championship
    val constructor = selectedTab == 1
    val rows = if (constructor) championship.data.constructors else championship.data.drivers
    val previous =
        if (constructor) championship.data.previousConstructors
        else championship.data.previousDrivers
    val info =
        if (constructor) championship.data.constructorsInfo else championship.data.driversInfo
    val holder = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            listOf(R.string.drivers, R.string.v2_constructors).forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabChange(index) },
                    text = { Text(stringResource(title)) },
                )
            }
        }
        holder.SaveableStateProvider(selectedTab) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { vm.refresh(force = true) },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "header") {
                        Text(
                            stringResource(R.string.v2_championship),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            listOf(
                                    stringResource(R.string.v2_season, data.year),
                                    info.round
                                        .takeIf(String::isNotBlank)
                                        ?.let { stringResource(R.string.v2_round, it) }
                                        .orEmpty(),
                                )
                                .filter(String::isNotBlank)
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if ((refreshing || championship.loading) && rows.isEmpty())
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                        DataNotice(info.stale, info.failed, info.updatedAt) {
                            vm.refresh(force = true)
                        }
                        if (!info.stale && !info.failed && info.updatedAt > 0)
                            Text(
                                stringResource(
                                    R.string.v2_updated,
                                    localDateTime(java.time.Instant.ofEpochMilli(info.updatedAt)),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        Text(
                            stringResource(
                                when {
                                    info.comparisonReady && info.historyStale ->
                                        R.string.champ_comparison_cached
                                    info.comparisonReady -> R.string.champ_comparison
                                    refreshing -> R.string.champ_history_loading
                                    else -> R.string.champ_history_unavailable
                                },
                                info.previousRound.orEmpty(),
                                info.round,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        TextButton(onClick = { vm.refresh(force = true) }) {
                            Text(stringResource(R.string.home_refresh))
                        }
                    }
                    if (rows.isEmpty() && !refreshing && !championship.loading)
                        item(key = "empty") {
                            Text(
                                stringResource(
                                    if (info.failed) R.string.home_load_error
                                    else R.string.home_no_standings
                                )
                            )
                        }
                    items(rows, key = { it.id }) { row ->
                        StandingItem(
                            row,
                            confirmedMovement(row, previous, info),
                            favorite =
                                if (constructor) row.id in favorites.constructors
                                else row.id in favorites.drivers,
                            constructor = constructor,
                            onOpen = { if (constructor) onTeam(row.id) else onDriver(row.id) },
                        )
                    }
                    // Mantém um único banner de teste depois da classificação, sem interromper as
                    // linhas.
                    if (rows.isNotEmpty())
                        item(key = "test_ad") {
                            BannerAdView("ca-app-pub-3940256099942544/6300978111")
                        }
                }
            }
        }
    }
}
