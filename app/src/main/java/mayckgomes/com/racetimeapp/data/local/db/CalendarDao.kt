package mayckgomes.com.racetimeapp.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mayckgomes.com.racetimeapp.data.local.db.entities.CalendarEntity

@Dao
interface CalendarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CalendarEntity>)

    @Query("SELECT * FROM calendar ORDER BY CAST(round AS INTEGER)")
    suspend fun all(): List<CalendarEntity>

    @Query("DELETE FROM calendar")
    suspend fun clear()
}
