package mayckgomes.com.racetimeapp.navgation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mayckgomes.com.racetimeapp.notifications.ReminderTarget
import mayckgomes.com.racetimeapp.presentation.circuits.CalendarDestination
import mayckgomes.com.racetimeapp.presentation.driver.DriverScreen
import mayckgomes.com.racetimeapp.presentation.main.MainScreen
import mayckgomes.com.racetimeapp.presentation.network.NetworkScreen
import mayckgomes.com.racetimeapp.presentation.team.TeamScreen

@Serializable object main

@Serializable data class driverRoute(val driverId: String)

@Serializable data class teamRoute(val teamID: String)

@Serializable object circuitsRoute

@Serializable object settingsRoute

@Serializable
data class grandPrixRoute(val season: Int, val round: String, val resultType: String? = null)

@Serializable object network

@Composable
fun Navgation(reminderTarget: ReminderTarget? = null, onReminderHandled: () -> Unit = {}) {

    val navControler = rememberNavController()

    NavHost(
        navControler,
        startDestination = main,
        builder = {
            composable<main> { MainScreen(navControler) }

            composable<driverRoute> {
                val arg = it.toRoute<driverRoute>()
                DriverScreen(navControler, arg.driverId)
            }

            composable<teamRoute> {
                val args = it.toRoute<teamRoute>()
                TeamScreen(navControler, args.teamID)
            }

            composable<circuitsRoute> {
                CalendarDestination(
                    onBack = { navControler.popBackStack() },
                    onGrandPrix = { year, round ->
                        navControler.navigate(grandPrixRoute(year, round))
                    },
                )
            }

            composable<settingsRoute> {
                mayckgomes.com.racetimeapp.presentation.settings.SettingsDestination(
                    onBack = { navControler.popBackStack() }
                )
            }
            composable<grandPrixRoute> {
                val args = it.toRoute<grandPrixRoute>()
                mayckgomes.com.racetimeapp.presentation.gp.GrandPrixScreen(
                    args.season,
                    args.round,
                    args.resultType,
                    onBack = { navControler.popBackStack() },
                    onDriver = { id -> navControler.navigate(driverRoute(id)) },
                )
            }
            composable<network> { NetworkScreen(navControler) }
        },
    )

    LaunchedEffect(reminderTarget) {
        reminderTarget?.let { target ->
            navControler.navigate(grandPrixRoute(target.season, target.round)) {
                launchSingleTop = true
            }
            onReminderHandled()
        }
    }
}
