package mayckgomes.com.racetimeapp.data.local

import android.content.SharedPreferences
import kotlinx.serialization.json.*

data class StoredResponse(val rows: List<JsonObject>, val savedAt: Long)

interface ResponseCache {
    fun read(key: String): StoredResponse?

    fun write(key: String, rows: List<JsonObject>, savedAt: Long)
}

class PreferenceResponseCache(private val prefs: SharedPreferences) : ResponseCache {
    override fun read(key: String): StoredResponse? {
        val value = prefs.getString("jolpica:$key", null) ?: return legacy(key)
        return runCatching {
                StoredResponse(
                    Json.parseToJsonElement(value).jsonArray.map { it.jsonObject },
                    prefs.getLong("jolpica:$key:time", 0),
                )
            }
            .getOrNull()
    }

    override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
        prefs
            .edit()
            .putString("jolpica:$key", JsonArray(rows).toString())
            .putLong("jolpica:$key:time", savedAt)
            .apply()
    }

    private fun legacy(key: String): StoredResponse? {
        // Migração de leitura: os dados antigos continuam intactos e sempre são considerados
        // desatualizados.
        val season = key.substringBefore('/')
        val oldKey =
            when (key.substringAfter('/')) {
                "races" -> "calendar_json"
                "driverstandings" -> "drivers_json"
                "constructorstandings" -> "constructors_json"
                "last/results" -> "last_json"
                else -> return null
            }
        val old = prefs.getString(oldKey, null) ?: return null
        return runCatching {
                val rows = Json.parseToJsonElement(old).jsonArray
                if (
                    oldKey == "calendar_json" &&
                        rows.any { (it.jsonObject["season"] as? JsonPrimitive)?.content != season }
                )
                    return null
                // Os caches antigos de ranking/resultado não guardavam temporada: não atribuímos um
                // ano inventado.
                if (oldKey != "calendar_json") return null
                StoredResponse(rows.map { it.jsonObject }, 0)
            }
            .getOrNull()
    }
}
