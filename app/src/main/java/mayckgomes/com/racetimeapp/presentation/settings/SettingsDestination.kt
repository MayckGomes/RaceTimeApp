@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mayckgomes.com.racetimeapp.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mayckgomes.com.racetimeapp.R

@Composable
fun SettingsDestination(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_tab)) },
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
        Box(Modifier.fillMaxSize().padding(padding)) { SettingsScreen(showHeading = false) }
    }
}
