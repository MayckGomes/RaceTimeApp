package mayckgomes.com.racetimeapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.data?.lastPathSegment ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler(context.applicationContext).deliver(id)
            } catch (_: RuntimeException) {
                ReminderStore(context).markSchedulingFailed(true)
            } finally {
                pending.finish()
            }
        }
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                setOf(
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_LOCALE_CHANGED,
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
                )
        )
            return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler(context.applicationContext).apply {
                    createChannel()
                    rebuild(forceRearm = true)
                }
                ReminderSyncWorker.configure(context, ReminderStore(context).settings().enabled)
                ReminderSyncWorker.refresh(context)
            } catch (_: RuntimeException) {
                ReminderStore(context).markSchedulingFailed(true)
            } finally {
                pending.finish()
            }
        }
    }
}
