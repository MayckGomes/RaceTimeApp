package mayckgomes.com.racetimeapp.presentation.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(navController: NavController, id: String) {

    val viewmodel = viewModel<teamViewModel>()

    val isLoading = viewmodel.isLoading.collectAsStateWithLifecycle().value

    val isFav = viewmodel.isFavorite.collectAsStateWithLifecycle().value

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    LaunchedEffect(id) {
        viewmodel.getInfo(id)
        viewmodel.verifyFav(context, id)
    }

    val teamInfo = viewmodel.teamInfo.collectAsStateWithLifecycle().value

    RaceTimeAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.v2_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.topappbar),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isFav) {

                                    scope.launch {
                                        viewmodel.delFav(context, id)
                                        viewmodel.isFavoriteFalse()
                                    }
                                } else {

                                    scope.launch {
                                        viewmodel.saveFav(context, id)
                                        viewmodel.isFavoriteTrue()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    if (isFav) Icons.Outlined.Favorite
                                    else Icons.Outlined.FavoriteBorder,
                                contentDescription =
                                    stringResource(
                                        if (isFav) R.string.fav_remove else R.string.fav_add
                                    ),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                )
            }
        ) {
            if (isLoading) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(it),
                ) {
                    CircularProgressIndicator()
                }
            } else {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(it)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Spacer(Modifier.size(40.dp))

                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(70.dp),
                    )

                    Spacer(Modifier.size(20.dp))

                    if (teamInfo.isNotEmpty()) {

                        Text(
                            text = teamInfo.first().Constructor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )

                        Spacer(Modifier.size(20.dp))

                        Text(
                            text = stringResource(R.string.nationality),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )

                        Spacer(Modifier.size(10.dp))

                        Text(text = teamInfo.first().Constructor.nationality)
                    } else {

                        Text(
                            text = stringResource(R.string.fav_profile_unavailable),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )
                        TextButton(onClick = { viewmodel.getInfo(id) }) {
                            Text(stringResource(R.string.home_retry))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DriverScreenPreview() {
    TeamScreen(rememberNavController(), "")
}
