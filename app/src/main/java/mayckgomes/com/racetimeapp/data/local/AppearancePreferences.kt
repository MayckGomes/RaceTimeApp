package mayckgomes.com.racetimeapp.data.local

import android.content.Context

enum class Appearance {
    SYSTEM,
    DARK,
    LIGHT,
}

class AppearancePreferences(context: Context) {
    val preferences =
        context.applicationContext.getSharedPreferences(
            "appearance_preferences",
            Context.MODE_PRIVATE,
        )

    fun current(): Appearance =
        runCatching { Appearance.valueOf(preferences.getString("theme", "SYSTEM") ?: "SYSTEM") }
            .getOrDefault(Appearance.SYSTEM)

    fun save(value: Appearance): Boolean =
        preferences.edit().putString("theme", value.name).commit()
}
