package mayckgomes.com.racetimeapp.domain.weekend

import java.time.Instant

data class ResultSnapshot(
    val result: PublishedResult,
    val stale: Boolean,
    val failed: Boolean,
    val updatedAt: Long,
)

// Aliases como "last" e número da rodada podem conter o mesmo resultado. Vale a cópia revalidada
// mais recentemente.
fun homeResults(
    calendar: List<RaceWeekend>,
    blocks: Collection<DataBlock<List<PublishedResult>>>,
): List<ResultSnapshot> =
    blocks
        .flatMap { block ->
            block.data
                .filter { it.entries.isNotEmpty() }
                .map { ResultSnapshot(it, block.stale, block.failed, block.updatedAt) }
        }
        .groupBy { it.result.key }
        .values
        .map { copies ->
            copies.maxWithOrNull(compareBy<ResultSnapshot> { it.updatedAt }.thenBy { !it.stale })!!
        }
        .sortedWith(
            compareByDescending<ResultSnapshot> { it.result.season.toIntOrNull() ?: 0 }
                .thenByDescending { it.result.round.toIntOrNull() ?: 0 }
                .thenByDescending { snapshot ->
                    resultStart(calendar, snapshot.result) ?: Instant.MIN
                }
                .thenByDescending { it.result.type == SessionType.RACE }
        )

fun resultStart(calendar: List<RaceWeekend>, result: PublishedResult): Instant? =
    calendar
        .firstOrNull { it.season == result.season && it.round == result.round }
        ?.sessions()
        ?.firstOrNull { it.type == result.type }
        ?.start

fun resultSessionDate(calendar: List<RaceWeekend>, result: PublishedResult): String? =
    calendar
        .firstOrNull { it.season == result.season && it.round == result.round }
        ?.sessions()
        ?.firstOrNull { it.type == result.type }
        ?.date

fun secondsUntil(target: Instant?, now: Instant): Long? =
    target?.let { (it.epochSecond - now.epochSecond).takeIf { seconds -> seconds > 0 } }

fun shouldShowNextRace(context: WeekendContext): Boolean =
    context.nextRace?.let { next ->
        context.weekend?.let { it.season != next.season || it.round != next.round } ?: true
    } ?: false
