package mayckgomes.com.racetimeapp.data.local

import android.content.SharedPreferences

class LocalCache(private val sharedPreferences: SharedPreferences) {
    fun hasData(): Boolean = sharedPreferences.all.isNotEmpty()
}
