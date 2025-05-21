package mayckgomes.com.racetimeapp.navgation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mayckgomes.com.racetimeapp.presentation.circuits.CircuitsScreen
import mayckgomes.com.racetimeapp.presentation.driver.DriverScreen
import mayckgomes.com.racetimeapp.presentation.main.MainScreen
import mayckgomes.com.racetimeapp.presentation.network.NetworkScreen
import mayckgomes.com.racetimeapp.presentation.team.TeamScreen

@Serializable
object main

@Serializable
data class driverRoute(
    val driverId: String
)

@Serializable
data class teamRoute(
    val teamID: String
)

@Serializable
object circuitsRoute

@Serializable
object network

@Composable
fun Navgation(haveInternet: Boolean){

    val navControler = rememberNavController()

    NavHost(navControler, startDestination = if (haveInternet) main else network, builder = {

        composable<main> {
            MainScreen(navControler)
        }

        composable<driverRoute>{
            val arg = it.toRoute<driverRoute>()
            DriverScreen(navControler,arg.driverId)
        }

        composable<teamRoute>{
            val args = it.toRoute<teamRoute>()
            TeamScreen(navControler,args.teamID)
        }

        composable<circuitsRoute>{
            CircuitsScreen(navControler)
        }

        composable<network>{
            NetworkScreen(navControler)
        }

    })

}