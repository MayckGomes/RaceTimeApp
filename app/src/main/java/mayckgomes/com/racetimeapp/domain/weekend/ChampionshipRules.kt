package mayckgomes.com.racetimeapp.domain.weekend

data class ChampionshipStandingInfo(
    val round: String = "",
    val previousRound: String? = null,
    val comparisonReady: Boolean = false,
    val historyStale: Boolean = false,
    val failed: Boolean = false,
    val stale: Boolean = false,
    val updatedAt: Long = 0,
)

// A ordem oficial vem da posição, nunca de um desempate inventado por pontos, nome ou vitórias.
fun orderedStandings(rows: List<Standing>): List<Standing> =
    rows
        .filter { it.id.isNotBlank() }
        .groupBy { it.id }
        .values
        .mapNotNull { copies -> copies.distinct().singleOrNull() }
        .sortedBy { it.position.toIntOrNull()?.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }

fun confirmedMovement(
    current: Standing,
    previous: List<Standing>,
    info: ChampionshipStandingInfo,
): Int? {
    val round = info.round.toIntOrNull() ?: return null
    val prior = info.previousRound?.toIntOrNull() ?: return null
    if (!info.comparisonReady || round <= 1 || prior != round - 1 || current.id.isBlank())
        return null
    return movement(current, previous)
}
