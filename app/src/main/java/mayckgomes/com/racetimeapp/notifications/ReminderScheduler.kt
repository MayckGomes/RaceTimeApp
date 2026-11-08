package mayckgomes.com.racetimeapp.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.*
import java.time.format.*
import mayckgomes.com.racetimeapp.MainActivity
import mayckgomes.com.racetimeapp.R
import mayckgomes.com.racetimeapp.domain.model.*
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar

class ReminderScheduler(
    private val context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val store = ReminderStore(context)
    private val alarms = requireNotNull(context.getSystemService(AlarmManager::class.java))
    private val notifications =
        requireNotNull(context.getSystemService(NotificationManager::class.java))

    companion object {
        const val CHANNEL = "race_sessions"
        private val lock = Any()
    }

    fun createChannel() {
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notifications_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    fun canNotify(): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            notifications.getNotificationChannel(CHANNEL)?.importance !=
                NotificationManager.IMPORTANCE_NONE

    fun canScheduleExactly(): Boolean =
        Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    fun updateSettings(settings: ReminderSettings): Boolean =
        synchronized(lock) {
            if (!store.saveSettings(settings)) return@synchronized false
            rebuild()
            true
        }

    fun updateCalendar(races: List<RacesCalendar>, updatedAt: Long = clock.millis()): Boolean =
        synchronized(lock) {
            if (updatedAt < store.lastSync() && store.lastSync() <= clock.millis())
                return@synchronized true
            if (!store.saveCalendar(races, updatedAt)) return@synchronized false
            rebuild()
            true
        }

    fun rebuild(forceRearm: Boolean = false) =
        synchronized(lock) {
            createChannel()
            val plan =
                planReminders(
                    store.calendar(),
                    store.settings(),
                    clock.instant(),
                    store.scheduled(),
                    store.delivered(),
                    canNotify(),
                    forceRearm,
                )
            plan.cancel.forEach { cancel(it) }
            // Persiste antes de armar: o receiver pode chegar imediatamente no modo "No horário".
            if (!store.saveScheduled(plan.active)) {
                store.markSchedulingFailed(true)
                return@synchronized
            }
            val failed = mutableSetOf<String>()
            plan.schedule.forEach { reminder ->
                val intent = pending(reminder.id, PendingIntent.FLAG_UPDATE_CURRENT)!!
                try {
                    if (canScheduleExactly()) {
                        try {
                            alarms.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminder.triggerMillis,
                                intent,
                            )
                        } catch (_: SecurityException) {
                            alarms.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminder.triggerMillis,
                                intent,
                            )
                        }
                    } else
                        alarms.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminder.triggerMillis,
                            intent,
                        )
                } catch (_: RuntimeException) {
                    failed += reminder.id
                    cancel(reminder)
                }
            }
            if (failed.isNotEmpty()) store.saveScheduled(plan.active.filterNot { it.id in failed })
            store.markSchedulingFailed(failed.isNotEmpty())
        }

    fun deliver(id: String) =
        synchronized(lock) {
            val reminder = store.scheduled().firstOrNull { it.id == id } ?: return@synchronized
            val now = clock.instant()
            if (
                !reminderIsDue(reminder, store.settings(), now) ||
                    reminderWasDelivered(reminder, store.delivered()) ||
                    !canNotify()
            )
                return@synchronized
            val (season, round) = reminder.destination ?: return@synchronized
            val openIntent =
                Intent(context, MainActivity::class.java)
                    .setData(Uri.parse("racetime://gp/$season/$round"))
                    .putExtra("reminder_season", season)
                    .putExtra("reminder_round", round)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val openActivity =
                PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val time =
                Instant.ofEpochMilli(reminder.startMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            val text =
                context.getString(
                    if (now.toEpochMilli() < reminder.startMillis) R.string.notify_starts_at
                    else R.string.notify_scheduled_at,
                    reminder.raceName,
                    time,
                )
            val notification =
                NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(
                        context.getString(
                            R.string.notify_title,
                            context.getString(sessionTitle(reminder.label)),
                        )
                    )
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(openActivity)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .build()
            try {
                NotificationManagerCompat.from(context).notify(reminder.eventKey, 0, notification)
                store.markDelivered(reminder)
                store.saveScheduled(store.scheduled().filterNot { it.id == id })
            } catch (_: SecurityException) {
                /* Permissão revogada entre a verificação e o envio. */
            }
        }

    private fun cancel(reminder: SessionReminder) {
        pending(reminder.id, PendingIntent.FLAG_NO_CREATE)?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    private fun pending(id: String, flag: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java)
                .setData(Uri.parse("racetime://reminder/${Uri.encode(id)}")),
            flag or PendingIntent.FLAG_IMMUTABLE,
        )
}

fun sessionTitle(label: String): Int =
    when (label) {
        "home_fp1" -> R.string.home_fp1
        "home_fp2" -> R.string.home_fp2
        "home_fp3" -> R.string.home_fp3
        "home_qualifying" -> R.string.home_qualifying
        "home_sprint" -> R.string.home_sprint
        "home_sprint_qualifying" -> R.string.home_sprint_qualifying
        else -> R.string.home_race
    }
