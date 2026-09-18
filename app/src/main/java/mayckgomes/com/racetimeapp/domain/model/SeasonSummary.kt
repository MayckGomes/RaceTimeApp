package mayckgomes.com.racetimeapp.domain.model

data class SeasonSummary(
    val season: String,
    val round: Int,
    val currentRaceName: String,
    val updatedAt: Long
)
