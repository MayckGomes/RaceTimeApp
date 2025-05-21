package mayckgomes.com.racetimeapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat.getSystemService

class TestNetwork {

    @Composable
    fun verifyNetwork(context: Context): Boolean {

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = cm.activeNetwork
        val networkCapabilities = cm.getNetworkCapabilities(network)

        return if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            true
        } else {
            false
        }
    }
}