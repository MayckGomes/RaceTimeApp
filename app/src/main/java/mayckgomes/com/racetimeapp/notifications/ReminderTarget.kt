package mayckgomes.com.racetimeapp.notifications

import android.content.Intent

data class ReminderTarget(val season: Int, val round: String)

fun reminderTarget(intent: Intent): ReminderTarget? {
    val uri = intent.data ?: return null
    if (uri.scheme != "racetime" || uri.host != "gp" || uri.pathSegments.size != 2) return null
    val season = uri.pathSegments[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
    val round = uri.pathSegments[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return ReminderTarget(season, round.toString())
}
