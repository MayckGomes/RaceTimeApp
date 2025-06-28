package mayckgomes.com.racetimeapp.presentation.circuits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.components.calendarItem.RaceItem
import mayckgomes.com.racetimeapp.navgation.main
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitsScreen(navController: NavController) {

    val viewModel = viewModel<circuitsViewModel>()

    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    val circuitsList = viewModel.circuitsList.collectAsStateWithLifecycle().value

    RaceTimeAppTheme{

        val actualWeekend by viewModel.actualWeekend.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier
                .fillMaxSize(),

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
                    ),

                    navigationIcon = {

                        IconButton(onClick = {

                            navController.popBackStack(main,false)

                        }) {

                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "back",
                                tint = MaterialTheme.colorScheme.background
                                )

                        }

                    }

                )

            }
        ) {

            if (isLoading){

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {

                    CircularProgressIndicator()

                }

            } else {

                if (circuitsList.isNotEmpty()){

                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .padding(it)
                            .padding(10.dp)

                    ) {
                        items(circuitsList) {

                            RaceItem(
                                actualWeekend = actualWeekend,
                                race = it
                            )
                            HorizontalDivider()
                            Spacer( modifier = Modifier.size(10.dp))

                        }
                    }

                } else {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = stringResource(R.string.conection)
                        )

                    }
                }
            }
        }
    }
}