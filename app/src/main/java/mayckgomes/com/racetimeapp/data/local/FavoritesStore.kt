package mayckgomes.com.racetimeapp.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FavoriteIds(val drivers: Map<String, String>, val constructors: Map<String, String>)

class FavoritesStore(context: Context) {
    // Mantém os arquivos e IDs das versões anteriores e observa mudanças feitas pelas telas de
    // detalhes.
    private val drivers = context.getSharedPreferences("driversFavorites", Context.MODE_PRIVATE)
    private val teams = context.getSharedPreferences("teamsFavorites", Context.MODE_PRIVATE)

    private fun read() =
        FavoriteIds(
            drivers.all.mapValues { it.value?.toString().orEmpty() },
            teams.all.mapValues { it.value?.toString().orEmpty() },
        )

    private val _ids = MutableStateFlow(read())
    val ids = _ids.asStateFlow()
    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _ids.value = read() }

    init {
        drivers.registerOnSharedPreferenceChangeListener(listener)
        teams.registerOnSharedPreferenceChangeListener(listener)
    }

    fun add(id: String, label: String, constructor: Boolean): Boolean {
        if (id.isBlank()) return false
        val saved = (if (constructor) teams else drivers).edit().putString(id, label).commit()
        if (saved) _ids.value = read()
        return saved
    }

    fun remove(id: String, constructor: Boolean): Boolean {
        if (id.isBlank()) return false
        val saved = (if (constructor) teams else drivers).edit().remove(id).commit()
        if (saved) _ids.value = read()
        return saved
    }
}
