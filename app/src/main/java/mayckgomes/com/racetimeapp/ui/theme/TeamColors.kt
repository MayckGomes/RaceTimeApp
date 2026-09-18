package mayckgomes.com.racetimeapp.ui.theme

import androidx.compose.ui.graphics.Color

private val teamColorsByConstructor = mapOf(
    "red_bull" to Color(0xFF3671C6),
    "ferrari" to Color(0xFFE8002D),
    "mclaren" to Color(0xFFFF8000),
    "mercedes" to Color(0xFF00D2BE),
    "aston_martin" to Color(0xFF229971),
    "alpine" to Color(0xFF0090FF),
    "williams" to Color(0xFF64C4FF),
    "rb" to Color(0xFF6692FF),
    "haas" to Color(0xFFB6BABD),
    "sauber" to Color(0xFF52E252),
    "alphatauri" to Color(0xFF2B4562),
)

fun constructorColor(constructorId: String?, fallback: Color = Color(0xFFBFC7D5)): Color {
    return teamColorsByConstructor[constructorId?.lowercase()] ?: fallback
}
