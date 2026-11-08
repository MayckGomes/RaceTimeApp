package mayckgomes.com.racetimeapp.ui.theme

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import mayckgomes.com.racetimeapp.data.local.Appearance
import mayckgomes.com.racetimeapp.data.local.AppearancePreferences

private val RaceTimeLightColors =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        error = LightError,
        onError = LightOnError,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
    )

private val RaceTimeDarkColors =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        error = DarkError,
        onError = DarkOnError,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
    )

@Composable
fun RaceTimeAppTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { AppearancePreferences(context) }
    var appearance by remember(store) { mutableStateOf(store.current()) }
    DisposableEffect(store) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                appearance = store.current()
            }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val dark =
        darkTheme
            ?: when (appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.DARK -> true
                Appearance.LIGHT -> false
            }
    val colorScheme = if (dark) RaceTimeDarkColors else RaceTimeLightColors

    MaterialTheme(colorScheme = colorScheme, typography = RaceTimeTypography, content = content)
}
