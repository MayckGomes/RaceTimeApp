package mayckgomes.com.racetimeapp

import android.app.Application
import mayckgomes.com.racetimeapp.di.appModule
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
    }
}
