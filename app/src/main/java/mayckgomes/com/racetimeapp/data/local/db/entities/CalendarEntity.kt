package mayckgomes.com.racetimeapp.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar")
data class CalendarEntity(
    @PrimaryKey val round: String,
    val season: String,
    val raceName: String,
    val date: String,
    val fetchedAt: Long
)
