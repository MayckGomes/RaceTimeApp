package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.navgation.circuitsRoute

@Composable
fun HomeScreen(navController: NavController) {
    HomeOverview(onCalendar = { navController.navigate(circuitsRoute) })
}
