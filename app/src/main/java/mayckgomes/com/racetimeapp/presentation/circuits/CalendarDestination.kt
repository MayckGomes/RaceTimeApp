@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.circuits

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mayckgomes.com.racetimeapp.R

@Composable
fun CalendarDestination(onBack: () -> Unit, onGrandPrix: (Int, String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_calendar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.v2_back),
                        )
                    }
                },
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            CalendarOverview(onGrandPrix, showTitle = false)
        }
    }
}
