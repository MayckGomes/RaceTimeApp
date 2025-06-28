package mayckgomes.com.racetimeapp.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import mayckgomes.com.racetimeapp.components.driversQualiResultTable.DriversQualiTable
import mayckgomes.com.racetimeapp.presentation.homeQuali.HomeQualiViewmodel

@Composable
fun HomeQualiScreenContent(
    navController: NavController,
){

    val viewmodel: HomeQualiViewmodel = viewModel()

    val circuitQualiName by viewmodel.circuitName.collectAsStateWithLifecycle()

    val lastQualiResults by viewmodel.lastResults.collectAsStateWithLifecycle()

    val isLoading by viewmodel.isLoading.collectAsStateWithLifecycle()

   if (isLoading){

       Column(
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.Center,
           modifier = Modifier.fillMaxSize()
       ){

           CircularProgressIndicator()

       }

   } else {

       Column(
           modifier = Modifier.fillMaxSize()
       ) {

           Spacer(Modifier.size(20.dp))

           Text(
               text = circuitQualiName,
               fontWeight = FontWeight.Bold,
               fontSize = MaterialTheme.typography.titleLarge.fontSize,
               modifier = Modifier.align(Alignment.CenterHorizontally)
           )

           Spacer(Modifier.size(15.dp))

           DriversQualiTable(navController, lastQualiResults)

       }

   }

}