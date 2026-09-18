package mayckgomes.com.racetimeapp.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_standings")
data class DriverStandingEntity(
    @PrimaryKey val driverId: String,
    val givenName: String,
    val familyName: String,
    val position: String,
    val points: String,
    val wins: String,
    val fetchedAt: Long
)
