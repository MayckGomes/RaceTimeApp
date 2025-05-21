package mayckgomes.com.racetimeapp.presentation.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.data.repository.TestNetwork
import mayckgomes.com.racetimeapp.navgation.main
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(navController: NavController) {

    val context = LocalContext.current

    val repository = TestNetwork()

    val viewmodel = viewModel<networkViewmodel>()

    val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

    RaceTimeAppTheme {

        Scaffold(
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
        ) {

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                        .verticalScroll(rememberScrollState())
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = "no Network",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )

                    Spacer(Modifier.size(20.dp))

                    Text(
                        text = stringResource(R.string.conection)
                    )
                }
            }

        }

        if (isLoading){

            if(repository.verifyNetwork(context)){
                navController.navigate(main)
            }

            viewmodel.isLoadingFalse()

        }
    }

}
@Preview(showSystemUi = true)
@Composable
private fun NetworkScreenPreview() {
    NetworkScreen(rememberNavController())
}