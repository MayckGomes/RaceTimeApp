package mayckgomes.com.racetimeapp.presentation.driver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.navgation.main
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(navController: NavController,id: String) {

    val viewmodel = viewModel<driverViewmodel>()

    val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

    val isFav = viewmodel.isFavorite.collectAsStateWithLifecycle().value

    val scope = CoroutineScope(Dispatchers.IO)

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        viewmodel.getInfo(id)
        viewmodel.verifyFav(context, id)

    }

    val driverInfo = viewmodel.driverInfo.collectAsStateWithLifecycle().value

    RaceTimeAppTheme {

        Scaffold(
            topBar = {

                TopAppBar(
                    navigationIcon = { Icon(
                        modifier = Modifier.clickable{

                            navController.popBackStack(main,false)

                        },
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "back",
                        tint = MaterialTheme.colorScheme.background
                    ) },
                    title = {
                        Text(
                            text = stringResource(R.string.topappbar),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    },

                    actions = {
                        IconButton(onClick = {

                            if (isFav){

                                scope.launch {
                                    viewmodel.delFav(context,id)
                                    viewmodel.isFavoriteFalse()
                                }

                            } else {

                                scope.launch {
                                    viewmodel.saveFav(context,id)
                                    viewmodel.isFavoriteTrue()
                                }

                            }

                        }) {

                            Icon(
                                imageVector = if (isFav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "icon",
                                tint = MaterialTheme.colorScheme.surface
                            )

                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )

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

               Column(
                   horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier
                       .fillMaxSize()
                       .padding(it)
               ) {

                   Spacer(Modifier.size(40.dp))

                   Icon(
                       imageVector = Icons.Outlined.AccountCircle,
                       contentDescription = "Person",
                       tint = MaterialTheme.colorScheme.primary,
                       modifier = Modifier.size(70.dp)
                   )


                   Spacer(Modifier.size(20.dp))

                   if (driverInfo.isNotEmpty()){

                       Text(
                           text = "${driverInfo.first().givenName} ${driverInfo.first().familyName}",
                           fontWeight = FontWeight.Bold,
                           fontSize = 24.sp
                       )

                       Spacer( Modifier.size(20.dp))

                       Text(
                           text = stringResource(R.string.number),
                           fontWeight = FontWeight.Bold,
                           fontSize = 24.sp
                       )

                       Spacer( Modifier.size(10.dp))

                       Text(
                           text = driverInfo.first().permanentNumber
                       )

                       Spacer( Modifier.size(20.dp))

                       Text(
                           text = stringResource(R.string.nationality),
                           fontWeight = FontWeight.Bold,
                           fontSize = 24.sp
                       )

                       Spacer( Modifier.size(10.dp))

                       Text(
                           text = driverInfo.first().nationality
                       )

                   } else {

                       Text(
                           text = "erro lista",
                           fontWeight = FontWeight.Bold,
                           fontSize = 24.sp
                       )

                   }

               }

           }

        }

    }

}

@Preview(showSystemUi = true)
@Composable
private fun DriverScreenPreview() {
    DriverScreen(rememberNavController(),"")
}