package mayckgomes.com.racetimeapp.domain.model

import java.time.Instant
import kotlinx.serialization.Serializable
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar

val reminderLeadTimes = listOf(0, 15, 30, 60)

@Serializable
data class ReminderSettings(
    val enabled: Boolean = false,
    val races: Boolean = true,
    val qualifying: Boolean = true,
    val practice: Boolean = false,
    val sprint: Boolean = true,
    val minutesBefore: Int = 30,
    // Defaults herdam as escolhas antigas, sem reativar categorias desativadas.
    val fp1: Boolean = practice,
    val fp2: Boolean = practice,
    val fp3: Boolean = practice,
    val sprintQualifying: Boolean = qualifying,
) {
    fun includes(label: String): Boolean =
        when (label) {
            "home_race" -> races
            "home_qualifying" -> qualifying
            "home_sprint_qualifying" -> sprintQualifying
            "home_fp1" -> fp1
            "home_fp2" -> fp2
            "home_fp3" -> fp3
            "home_sprint" -> sprint
            else -> false
        }

    fun normalized() = copy(minutesBefore = minutesBefore.takeIf { it in reminderLeadTimes } ?: 30)
}

@Serializable
data class SessionReminder(
    val id: String,
    val raceName: String,
    val label: String,
    val startMillis: Long,
    val triggerMillis: Long,
    val season: String = "",
    val round: String = "",
) {
    // Registros antigos não continham season/round, mas seu ID já continha ambos.
    val eventSeason: String
        get() = season.ifBlank { id.split(':').getOrNull(0).orEmpty() }

    val eventRound: String
        get() = round.ifBlank { id.split(':').getOrNull(1).orEmpty() }

    val eventKey: String
        get() = "$eventSeason:$eventRound:$label"

    val destination: Pair<Int, String>?
        get() {
            val year = eventSeason.toIntOrNull()?.takeIf { it > 0 } ?: return null
            val roundNumber = eventRound.toIntOrNull()?.takeIf { it > 0 } ?: return null
            return year to roundNumber.toString()
        }
}

private fun calendarReminders(
    calendar: List<RacesCalendar>,
    settings: ReminderSettings,
): List<SessionReminder> {
    if (!settings.enabled) return emptyList()
    val lead = settings.normalized().minutesBefore
    return calendar
        .filter {
            it.season.toIntOrNull()?.let { y -> y > 0 } == true &&
                it.round.toIntOrNull()?.let { r -> r > 0 } == true
        }
        .flatMap { race ->
            raceSessions(race)
                .filter { settings.includes(it.label) }
                .map { session ->
                    SessionReminder(
                        "${race.season}:${race.round}:${session.label}:${session.start.epochSecond}",
                        race.raceName,
                        session.label,
                        session.start.toEpochMilli(),
                        session.start.minusSeconds(lead * 60L).toEpochMilli(),
                        race.season,
                        race.round,
                    )
                }
        }
        .distinctBy { it.eventKey }
        .sortedBy { it.triggerMillis }
}

fun buildReminders(
    calendar: List<RacesCalendar>,
    settings: ReminderSettings,
    now: Instant,
): List<SessionReminder> =
    calendarReminders(calendar, settings).filter { it.triggerMillis > now.toEpochMilli() }

// Tolerância de entrega do aviso, não duração estimada/oficial da sessão.
const val REMINDER_DELIVERY_GRACE = 5 * 60_000L

fun reminderIsDue(reminder: SessionReminder, settings: ReminderSettings, now: Instant): Boolean =
    settings.enabled &&
        settings.includes(reminder.label) &&
        reminder.destination != null &&
        now.toEpochMilli() >= reminder.triggerMillis &&
        now.toEpochMilli() - reminder.startMillis < REMINDER_DELIVERY_GRACE

fun reminderWasDelivered(reminder: SessionReminder, delivered: Set<String>): Boolean =
    reminder.id in delivered ||
        "event:${reminder.eventKey}" in delivered ||
        delivered.any { it.startsWith("${reminder.eventKey}:") }

data class ReminderPlan(
    val cancel: List<SessionReminder>,
    val schedule: List<SessionReminder>,
    val active: List<SessionReminder>,
)

fun planReminders(
    calendar: List<RacesCalendar>,
    settings: ReminderSettings,
    now: Instant,
    existing: List<SessionReminder>,
    delivered: Set<String>,
    notificationsAllowed: Boolean,
    forceRearm: Boolean = false,
): ReminderPlan {
    val desired =
        if (!notificationsAllowed) emptyList()
        else
            calendarReminders(calendar, settings)
                .filter { reminder ->
                    !reminderWasDelivered(reminder, delivered) &&
                        (reminder.triggerMillis > now.toEpochMilli() ||
                            // Um rebuild não perde um alarme já devido que ainda aguarda o
                            // receiver. Não cria catch-up novo.
                            existing.any {
                                it.id == reminder.id && it.triggerMillis == reminder.triggerMillis
                            } && reminderIsDue(reminder, settings, now))
                }
                .take(64)
    fun same(a: SessionReminder, b: SessionReminder) =
        a.id == b.id && a.triggerMillis == b.triggerMillis
    return ReminderPlan(
        existing.filter { old -> desired.none { same(old, it) } },
        desired.filter { next -> forceRearm || existing.none { same(it, next) } },
        desired,
    )
}
