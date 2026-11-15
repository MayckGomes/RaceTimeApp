@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.favorites

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.favorites.FavoriteCard
import mayckgomes.com.racetimeapp.components.home.DataNotice
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.navgation.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    navControler: NavHostController,
    onBrowseDrivers: () -> Unit,
    onBrowseTeams: () -> Unit,
) {
    val vm: FavoritesViewModel = koinViewModel()
    val data by vm.data.collectAsStateWithLifecycle()
    val ids by vm.ids.collectAsStateWithLifecycle()
    val details by vm.details.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    val constructor = selected == 1
    val selectedIds = if (constructor) ids.constructors else ids.drivers
    val championship = data.championship
    val standings = if (constructor) championship.data.constructors else championship.data.drivers
    val info =
        if (constructor) championship.data.constructorsInfo else championship.data.driversInfo
    val rows =
        remember(selectedIds, standings, details, constructor) {
            favoriteSummaries(selectedIds, standings, details, constructor)
        }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.refresh()
            awaitCancellation()
        }
    }
    val holder = rememberSaveableStateHolder()
    fun refresh() {
        reload++
        vm.refresh(force = true)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selected) {
                listOf(R.string.drivers, R.string.v2_constructors).forEachIndexed { index, title ->
                    Tab(
                        selected = selected == index,
                        onClick = { selected = index },
                        text = { Text(stringResource(title)) },
                    )
                }
            }
            holder.SaveableStateProvider(selected) {
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = ::refresh,
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
                                stringResource(R.string.myfavorites),
                                style = MaterialTheme.typography.displayLarge,
                            )
                            Text(
                                stringResource(R.string.fav_count, rows.size),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            DataNotice(info.stale, info.failed, info.updatedAt, ::refresh)
                            TextButton(
                                onClick = if (constructor) onBrowseTeams else onBrowseDrivers
                            ) {
                                Text(stringResource(R.string.fav_choose))
                            }
                        }
                        if (rows.isEmpty())
                            item(key = "empty") {
                                Text(
                                    stringResource(
                                        if (constructor) R.string.fav_empty_teams
                                        else R.string.fav_empty_drivers
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    stringResource(R.string.fav_empty_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        items(rows, key = { favoriteKey(it.id, constructor) }) { row ->
                            // Apenas cards visíveis/pré-carregados enriquecem seu
                            // cadastro/histórico. Não há polling de favoritos.
                            LaunchedEffect(row.id, constructor, reload, data.year) {
                                vm.loadDetails(row.id, constructor, generation = reload)
                            }
                            FavoriteCard(
                                row,
                                data.year,
                                championship.loading || refreshing,
                                info.failed,
                                onOpen = {
                                    if (constructor) navControler.navigate(teamRoute(row.id))
                                    else navControler.navigate(driverRoute(row.id))
                                },
                                onRemove = {
                                    val label = selectedIds[row.id] ?: row.name
                                    scope.launch {
                                        val saved = vm.remove(row.id, constructor)
                                        if (!saved)
                                            snackbar.showSnackbar(
                                                context.getString(R.string.fav_save_failed)
                                            )
                                        else if (
                                            snackbar.showSnackbar(
                                                context.getString(R.string.fav_removed, row.name),
                                                actionLabel = context.getString(R.string.fav_undo),
                                            ) == SnackbarResult.ActionPerformed
                                        ) {
                                            if (!vm.restore(row.id, label, constructor))
                                                snackbar.showSnackbar(
                                                    context.getString(R.string.fav_save_failed)
                                                )
                                        }
                                    }
                                },
                                onRetry = { vm.loadDetails(row.id, constructor, true) },
                                onResult = { result ->
                                    navControler.navigate(
                                        grandPrixRoute(
                                            result.season.toInt(),
                                            result.round,
                                            SessionType.RACE.name,
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
    }
}
