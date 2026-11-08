package mayckgomes.com.racetimeapp.domain.weekend

import java.util.Locale

private val retirementStatuses =
    setOf(
        "retired",
        "accident",
        "collision",
        "collision damage",
        "engine",
        "gearbox",
        "transmission",
        "clutch",
        "hydraulics",
        "electrical",
        "suspension",
        "brakes",
        "spun off",
        "puncture",
        "overheating",
        "mechanical",
        "driveshaft",
        "differential",
        "wheel",
        "throttle",
        "exhaust",
        "oil leak",
        "water leak",
        "fuel pressure",
        "fuel system",
        "power unit",
        "ers",
        "battery",
        "damage",
        "steering",
        "out of fuel",
        "tyre",
        "tyres",
    )
private val lapDurationPattern = Regex("[0-9]+(?::[0-5][0-9]){0,2}\\.[0-9]{3}")

enum class FinishCategory {
    FINISHED,
    RETIRED,
    DISQUALIFIED,
    NOT_STARTED,
    OTHER,
}

fun finishCategory(entry: ResultEntry): FinishCategory {
    val normalizedStatus = entry.status.trim().lowercase(Locale.ROOT)
    return when {
        entry.classifiedFinish -> FinishCategory.FINISHED
        normalizedStatus in setOf("disqualified", "excluded") -> FinishCategory.DISQUALIFIED
        normalizedStatus in
            setOf(
                "did not start",
                "did not qualify",
                "did not prequalify",
                "withdrawn",
                "107% rule",
            ) -> FinishCategory.NOT_STARTED
        normalizedStatus in retirementStatuses -> FinishCategory.RETIRED
        else -> FinishCategory.OTHER
    }
}

fun officialPosition(entry: ResultEntry): Int? {
    val position = entry.position.toIntOrNull()?.takeIf { it > 0 } ?: return null
    if (entry.positionText.isNotBlank() && entry.positionText.toIntOrNull() != position) return null
    return position
}

fun timingMillis(value: String): Long? {
    if (!value.matches(lapDurationPattern)) return null
    return runCatching {
            val timeParts = value.split(':')
            val secondAndMillisecondParts = timeParts.last().split('.')
            var durationInMillis =
                Math.addExact(
                    Math.multiplyExact(secondAndMillisecondParts[0].toLong(), 1000L),
                    secondAndMillisecondParts[1].toLong(),
                )
            if (timeParts.size >= 2)
                durationInMillis =
                    Math.addExact(
                        durationInMillis,
                        Math.multiplyExact(timeParts[timeParts.size - 2].toLong(), 60_000L),
                    )
            if (timeParts.size == 3)
                durationInMillis =
                    Math.addExact(
                        durationInMillis,
                        Math.multiplyExact(timeParts.first().toLong(), 3600_000L),
                    )
            durationInMillis.takeIf { it > 0 }
        }
        .getOrNull()
}

data class PositionChange(val entry: ResultEntry, val delta: Int)

data class RaceStatistics(
    val podium: List<ResultEntry>,
    val fastest: ResultEntry?,
    val mostGained: List<PositionChange>,
    val changes: Map<String, Int>,
    val groups: Map<FinishCategory, List<ResultEntry>>,
)

fun raceStatistics(result: PublishedResult): RaceStatistics? {
    if (result.type !in setOf(SessionType.RACE, SessionType.SPRINT) || result.entries.isEmpty())
        return null
    val uniqueDriverResults =
        result.entries
            .filter { it.driverId.isNotBlank() }
            .groupBy { it.driverId }
            .values
            .mapNotNull { it.singleOrNull() }
    val positionCounts =
        uniqueDriverResults.mapNotNull { officialPosition(it) }.groupingBy { it }.eachCount()
    val gridPositionCounts =
        uniqueDriverResults
            .mapNotNull { it.grid.toIntOrNull()?.takeIf { n -> n > 0 } }
            .groupingBy { it }
            .eachCount()
    val positionChanges =
        uniqueDriverResults
            .mapNotNull { entry ->
                val position = officialPosition(entry) ?: return@mapNotNull null
                val grid = entry.grid.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                if (
                    !entry.classifiedFinish ||
                        gridPositionCounts[grid] != 1 ||
                        positionCounts[position] != 1
                )
                    null
                else entry.driverId to (grid - position)
            }
            .toMap()
    val highestPositionGain = positionChanges.values.filter { it > 0 }.maxOrNull()
    val podium =
        uniqueDriverResults
            .filter {
                officialPosition(it)?.let { pos -> pos in 1..3 && positionCounts[pos] == 1 } ==
                    true &&
                    finishCategory(it) !in
                        setOf(FinishCategory.DISQUALIFIED, FinishCategory.NOT_STARTED)
            }
            .sortedBy { officialPosition(it) }
    val fastest =
        uniqueDriverResults
            .filter {
                it.fastestRank == "1" &&
                    timingMillis(it.fastestTime) != null &&
                    finishCategory(it) != FinishCategory.DISQUALIFIED
            }
            .singleOrNull()
    return RaceStatistics(
        podium,
        fastest,
        uniqueDriverResults.mapNotNull { entry ->
            positionChanges[entry.driverId]
                ?.takeIf { it == highestPositionGain }
                ?.let { PositionChange(entry, it) }
        },
        positionChanges,
        uniqueDriverResults.groupBy(::finishCategory),
    )
}

data class PitStop(
    val driverId: String,
    val stop: String,
    val lap: String,
    val duration: String,
    val time: String,
)

data class LapTiming(val driverId: String, val lap: Int, val position: String, val time: String)

fun supportsPitStops(season: Int): Boolean = season >= 2011

fun supportsLapTimes(season: Int): Boolean = season >= 1996

fun canLoadRaceStatistics(result: PublishedResult?, season: Int, round: String): Boolean =
    result?.type == SessionType.RACE &&
        result.season == season.toString() &&
        result.round == round &&
        result.entries.isNotEmpty()
