@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.home.RaceTimeHeader
import mayckgomes.com.racetimeapp.navgation.*
import mayckgomes.com.racetimeapp.presentation.circuits.CalendarOverview
import mayckgomes.com.racetimeapp.presentation.favorites.FavoritesScreen
import mayckgomes.com.racetimeapp.presentation.home.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(navControler: NavHostController) {
    val vm: HomeViewModel = koinViewModel()
    val data by vm.racingData.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var championshipTab by rememberSaveable { mutableIntStateOf(0) }
    val holder = rememberSaveableStateHolder()
    val titles =
        listOf(
            R.string.home,
            R.string.home_calendar,
            R.string.v2_championship,
            R.string.myfavorites,
        )
    val icons =
        listOf(
            Icons.Outlined.Home,
            Icons.Outlined.DateRange,
            Icons.Outlined.List,
            Icons.Outlined.Star,
        )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                RaceTimeHeader(
                    data.year,
                    onSettings = { navControler.navigate(settingsRoute) { launchSingleTop = true } },
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = {
                            Text(
                                stringResource(title),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                            )
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor =
                                    MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                            ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            holder.SaveableStateProvider(selected) {
                when (selected) {
                    0 ->
                        HomeOverview(
                            onCalendar = { selected = 1 },
                            onDrivers = {
                                championshipTab = 0
                                selected = 2
                            },
                            onTeams = {
                                championshipTab = 1
                                selected = 2
                            },
                            onFavorites = { selected = 3 },
                            onGrandPrix = { year, round, type ->
                                navControler.navigate(grandPrixRoute(year, round, type?.name))
                            },
                            onDriver = { navControler.navigate(driverRoute(it)) },
                            onTeam = { navControler.navigate(teamRoute(it)) },
                        )
                    1 ->
                        CalendarOverview(
                            onGrandPrix = { year, round ->
                                navControler.navigate(grandPrixRoute(year, round))
                            }
                        )
                    2 ->
                        mayckgomes.com.racetimeapp.presentation.championship.ChampionshipScreen(
                            selectedTab = championshipTab,
                            onTabChange = { championshipTab = it },
                            onDriver = { navControler.navigate(driverRoute(it)) },
                            onTeam = { navControler.navigate(teamRoute(it)) },
                        )
                    3 ->
                        FavoritesScreen(
                            navControler,
                            onBrowseDrivers = {
                                championshipTab = 0
                                selected = 2
                            },
                            onBrowseTeams = {
                                championshipTab = 1
                                selected = 2
                            },
                        )
                }
            }
        }
    }
    BackHandler(enabled = selected != 0) { selected = 0 }
}
