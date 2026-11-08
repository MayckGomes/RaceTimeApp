package mayckgomes.com.racetimeapp.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import org.koin.core.context.GlobalContext

class ReminderSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = ReminderStore(applicationContext)
        if (!store.settings().enabled) return Result.success()
        return try {
            val repository =
                GlobalContext.get()
                    .get<mayckgomes.com.racetimeapp.data.repository.RacingRepository>()
            val result = repository.calendar(repository.activeYear, force = true)
            if (result.failed || result.stale) failed(store)
            else {
                val saved =
                    ReminderScheduler(applicationContext)
                        .updateCalendar(result.data, result.updatedAt)
                if (saved) Result.success() else failed(store)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed(store)
        }
    }

    private fun failed(store: ReminderStore): Result {
        store.markSyncFailed(true)
        return if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        private const val PERIODIC = "session_calendar_periodic"
        private const val REFRESH = "session_calendar_refresh"

        private fun constraints() =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun configure(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) {
                manager.cancelUniqueWork(PERIODIC)
                manager.cancelUniqueWork(REFRESH)
                return
            }
            val work =
                PeriodicWorkRequestBuilder<ReminderSyncWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()
            manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, work)
        }

        fun refresh(context: Context) {
            if (!ReminderStore(context).settings().enabled) return
            val work =
                OneTimeWorkRequestBuilder<ReminderSyncWorker>()
                    .setConstraints(constraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(REFRESH, ExistingWorkPolicy.KEEP, work)
        }
    }
}
