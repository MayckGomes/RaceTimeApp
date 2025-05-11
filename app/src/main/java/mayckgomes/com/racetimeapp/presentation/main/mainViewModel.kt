package mayckgomes.com.racetimeapp.presentation.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import mayckgomes.com.racetimeapp.R

class MainViewModel: ViewModel(){

    data class Screens(
        val name: Int,
        val icon: ImageVector
    )

    val listScreens = listOf(
        Screens(R.string.drivers, Icons.Outlined.Person),
        Screens(R.string.home, Icons.Outlined.Home),
        Screens(R.string.teams, Icons.Outlined.Build),
        Screens(R.string.myfavorites, Icons.Outlined.Star),
    )

}