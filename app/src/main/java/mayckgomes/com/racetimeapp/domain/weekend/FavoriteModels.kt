package mayckgomes.com.racetimeapp.domain.weekend

data class FavoriteProfile(
    val id: String,
    val name: String,
    val number: String = "",
    val nationality: String = "",
    val dateOfBirth: String = "",
    val code: String = "",
)

data class FavoriteDetails(
    val profile: DataBlock<FavoriteProfile?> = DataBlock(null),
    val recentRace: DataBlock<PublishedResult?> = DataBlock(null),
    val checkedAt: Long = 0,
    val year: Int = 0,
    val loading: Boolean = false,
)

data class FavoriteSummary(
    val id: String,
    val name: String,
    val constructor: Boolean,
    val standing: Standing?,
    val profile: FavoriteProfile?,
    val details: FavoriteDetails?,
)

fun favoriteSummaries(
    ids: Map<String, String>,
    standings: List<Standing>,
    details: Map<String, FavoriteDetails>,
    constructor: Boolean,
): List<FavoriteSummary> =
    ids.map { (id, savedName) ->
            val standing = standings.singleOrNull { it.id == id }
            val info = details[favoriteKey(id, constructor)]
            FavoriteSummary(
                id,
                standing?.name?.takeIf(String::isNotBlank)
                    ?: info?.profile?.data?.name?.takeIf(String::isNotBlank)
                    ?: savedName.ifBlank { id },
                constructor,
                standing,
                info?.profile?.data,
                info,
            )
        }
        .sortedWith(
            compareBy<FavoriteSummary> {
                    it.standing?.position?.toIntOrNull()?.takeIf { pos -> pos > 0 } ?: Int.MAX_VALUE
                }
                .thenBy { it.name.lowercase(java.util.Locale.ROOT) }
                .thenBy { it.id }
        )

fun favoriteKey(id: String, constructor: Boolean): String =
    (if (constructor) "constructor:" else "driver:") + id

fun favoriteBestEntry(result: PublishedResult, id: String, constructor: Boolean): ResultEntry? =
    result.entries
        .filter { if (constructor) it.constructorId == id else it.driverId == id }
        .minByOrNull { it.position.toIntOrNull()?.takeIf { pos -> pos > 0 } ?: Int.MAX_VALUE }
