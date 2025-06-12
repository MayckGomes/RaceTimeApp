package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.driversQualiResultTable.DriversQualiTable
import mayckgomes.com.racetimeapp.components.driversResultTable.DriversTable
import mayckgomes.com.racetimeapp.navgation.circuitsRoute
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val scope = rememberCoroutineScope()

    val viewmodel = viewModel<homeViewmodel>()

    val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

    val circuitRaceName = viewmodel.circuitName.collectAsStateWithLifecycle().value

    val lastRaceResults = viewmodel.resultsList.collectAsStateWithLifecycle().value

    val circuitQualiRaceName = viewmodel.circuitQualiName.collectAsStateWithLifecycle().value

    val lastQualiResults = viewmodel.resultsQualiList.collectAsStateWithLifecycle().value

    val pagerState = rememberPagerState(pageCount = {2})

    RaceTimeAppTheme {

        if (isLoading){

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator()
            }

        } else {

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {viewmodel.getLastResults()},
                state = rememberPullToRefreshState(),

            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize(1f)
                        .systemBarsPadding()
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    Text(
                        modifier = Modifier.align(Alignment.Start),
                        text = stringResource(R.string.last_results),
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
                    )

                    Spacer(Modifier.size(10.dp))

                    TextButton (
                        onClick = {
                            navController.navigate(circuitsRoute)
                        }
                    ){
                        Text(
                            text = stringResource(R.string.see_calenar),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.size(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(1f)
                    ) {

                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth(0.5f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if(pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                            ),
                            onClick = {

                                if (pagerState.currentPage != 0){
                                    scope.launch { pagerState.scrollToPage(0) }
                                }

                            }
                        ) {
                            Text(
                                text = stringResource(R.string.Quali),
                                color = if(pagerState.currentPage == 0) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.size(10.dp))

                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if(pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                            ),
                            onClick = {

                                if (pagerState.currentPage != 1){
                                    scope.launch { pagerState.scrollToPage(1) }
                                }

                            }
                        ) {
                            Text(
                                text = stringResource(R.string.race),
                                color = if(pagerState.currentPage == 1) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.size(10.dp))

                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState
                    ) {

                        when{

                            pagerState.currentPage == 0 -> HomeScreenContent(
                                isQuali = true,
                                navController = navController,
                                circuitRaceName = circuitRaceName,
                                circuitQualiRaceName = circuitQualiRaceName,
                                lastRaceResults = lastRaceResults,
                                lastQualiResults = lastQualiResults
                            )
                            pagerState.currentPage == 1 -> HomeScreenContent(
                                isQuali = false,
                                navController = navController,
                                circuitRaceName = circuitRaceName,
                                circuitQualiRaceName = circuitQualiRaceName,
                                lastRaceResults = lastRaceResults,
                                lastQualiResults = lastQualiResults
                            )

                        }

                    }

                }

            }

        }

    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(rememberNavController())
}