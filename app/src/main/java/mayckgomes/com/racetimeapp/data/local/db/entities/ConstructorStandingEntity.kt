package mayckgomes.com.racetimeapp.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "constructor_standings")
data class ConstructorStandingEntity(
    @PrimaryKey val constructorId: String,
    val name: String,
    val position: String,
    val points: String,
    val fetchedAt: Long
)
