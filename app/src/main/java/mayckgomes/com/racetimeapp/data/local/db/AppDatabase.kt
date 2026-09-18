package mayckgomes.com.racetimeapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import mayckgomes.com.racetimeapp.data.local.db.CalendarDao
import mayckgomes.com.racetimeapp.data.local.db.StandingsDao
import mayckgomes.com.racetimeapp.data.local.db.entities.CalendarEntity
import mayckgomes.com.racetimeapp.data.local.db.entities.DriverStandingEntity
import mayckgomes.com.racetimeapp.data.local.db.entities.ConstructorStandingEntity

@Database(
    entities = [CalendarEntity::class, DriverStandingEntity::class, ConstructorStandingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao
    abstract fun standingsDao(): StandingsDao
}
