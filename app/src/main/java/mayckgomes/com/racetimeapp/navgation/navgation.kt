package mayckgomes.com.racetimeapp.navgation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import mayckgomes.com.racetimeapp.presentation.main.MainScreen

@Serializable
object main

@Serializable
object driver

@Serializable
object team

@Composable
fun Navgation(){

    val navControler = rememberNavController()

    NavHost(navControler, startDestination = main, builder = {

        composable<main> {
            MainScreen()
        }

    })

}