package mayckgomes.com.racetimeapp.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.presentation.drivers.DriversScreen
import mayckgomes.com.racetimeapp.presentation.favorites.FavoritesScreen
import mayckgomes.com.racetimeapp.presentation.home.HomeScreen
import mayckgomes.com.racetimeapp.presentation.teams.TeamsScreen
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navControler: NavHostController) {

    RaceTimeAppTheme {

        val viewmodel = viewModel<MainViewModel>()

        val pages = rememberPagerState(initialPage = 1, pageCount = {4})

        val listScreens = viewmodel.listScreens

        Scaffold(
            modifier = Modifier.fillMaxSize(1f),
            topBar = {
                TopAppBar(

                    title = {
                        Text(
                            text = stringResource(R.string.topappbar),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )

                )


            },

            bottomBar = {

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    listScreens.forEachIndexed { index, screen ->

                        NavigationBarItem(
                            selected = index == pages.currentPage,
                            onClick = {
                                CoroutineScope(Dispatchers.Main).launch{
                                    pages.scrollToPage(index)
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = "Icon")},
                            label = {
                                Text(stringResource(screen.name))
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surface,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.primary,
                            )
                        )

                    }
                }

            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(1f),
                    state = pages,
                    userScrollEnabled = false
                ) {

                    when(pages.currentPage){
                        0 -> DriversScreen(navControler)
                        1 -> HomeScreen(navControler)
                        2 -> TeamsScreen(navControler)
                        3 -> FavoritesScreen(navControler)
                    }

                }

                BackHandler {

                }

            }
        }

    }

}

@Preview(showSystemUi = true)
@Composable
private fun MainScreenPreview() {
    MainScreen(rememberNavController())
}