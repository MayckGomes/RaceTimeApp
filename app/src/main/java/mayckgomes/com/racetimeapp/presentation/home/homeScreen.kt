package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.driversResultTable.DriversTable
import mayckgomes.com.racetimeapp.navgation.circuitsRoute
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val viewmodel = viewModel<homeViewmodel>()

    val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

    val circuitName = viewmodel.circuitName.collectAsStateWithLifecycle().value

    val lastResults = viewmodel.resultsList.collectAsStateWithLifecycle().value

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

                    Spacer(Modifier.size(20.dp))

                    Text(
                        text = circuitName,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
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

                    Spacer(Modifier.size(15.dp))

                    DriversTable(navController,lastResults)

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