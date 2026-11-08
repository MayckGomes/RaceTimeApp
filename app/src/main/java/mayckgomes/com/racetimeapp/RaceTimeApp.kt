package mayckgomes.com.racetimeapp

import android.app.Application
import kotlinx.coroutines.launch
import mayckgomes.com.racetimeapp.di.appModule
import mayckgomes.com.racetimeapp.notifications.ReminderScheduler
import mayckgomes.com.racetimeapp.notifications.ReminderSyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RaceTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@RaceTimeApp)
            modules(appModule)
        }
        ReminderScheduler(this).createChannel()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { ReminderScheduler(this@RaceTimeApp).rebuild(forceRearm = true) }
                .onFailure {
                    mayckgomes.com.racetimeapp.notifications
                        .ReminderStore(this@RaceTimeApp)
                        .markSchedulingFailed(true)
                }
            ReminderSyncWorker.configure(
                this@RaceTimeApp,
                mayckgomes.com.racetimeapp.notifications
                    .ReminderStore(this@RaceTimeApp)
                    .settings()
                    .enabled,
            )
        }
    }
}
