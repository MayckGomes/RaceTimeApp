package mayckgomes.com.racetimeapp.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mayckgomes.com.racetimeapp.data.local.db.entities.ConstructorStandingEntity
import mayckgomes.com.racetimeapp.data.local.db.entities.DriverStandingEntity

@Dao
interface StandingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(items: List<DriverStandingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConstructors(items: List<ConstructorStandingEntity>)

    @Query("SELECT * FROM driver_standings ORDER BY CAST(position AS INTEGER)")
    suspend fun drivers(): List<DriverStandingEntity>

    @Query("SELECT * FROM constructor_standings ORDER BY CAST(position AS INTEGER)")
    suspend fun constructors(): List<ConstructorStandingEntity>

    @Query("DELETE FROM driver_standings")
    suspend fun clearDrivers()

    @Query("DELETE FROM constructor_standings")
    suspend fun clearConstructors()
}
