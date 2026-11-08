package mayckgomes.com.racetimeapp

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import java.time.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.local.*
import mayckgomes.com.racetimeapp.data.remote.JolpicaGateway
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class HomeCacheRestoreTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC)

    private class MemoryCache : ResponseCache {
        private val values = java.util.concurrent.ConcurrentHashMap<String, StoredResponse>()

        override fun read(key: String) = values[key]

        override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
            values[key] = StoredResponse(rows, savedAt)
        }
    }

    private val calendar =
        """{"season":"2026","round":"1","raceName":"Fixture GP","date":"2026-04-01","time":"14:00:00Z"}"""

    private fun MemoryCache.seed(at: Long) {
        fun save(route: String, row: String) =
            write(route, listOf(Json.parseToJsonElement(row).jsonObject), at)
        save("2026/races", calendar)
        save(
            "2026/driverstandings",
            """{"season":"2026","round":"1","DriverStandings":[{"position":"1","points":"25","Driver":{"driverId":"one","givenName":"Fixture","familyName":"Driver"}}]}""",
        )
        save(
            "2026/constructorstandings",
            """{"season":"2026","round":"1","ConstructorStandings":[{"position":"1","points":"25","Constructor":{"constructorId":"team","name":"Fixture Team"}}]}""",
        )
        save(
            "2026/1/results",
            """{"season":"2026","round":"1","raceName":"Fixture GP","date":"2026-04-01","Results":[{"position":"1","Driver":{"driverId":"one"}}]}""",
        )
    }

    @Test
    fun cachedDashboardBecomesAvailableWithoutIssuingAnyHttpRequest() = runTest {
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    error("must not fetch during restore")
                }
            )
        val cache = MemoryCache().apply { seed(clock.millis()) }
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.restoreHomeCache()
            val data = repo.data.value
            assertEquals("Fixture GP", data.calendar.data.single().raceName)
            assertEquals("Fixture Driver", data.championship.data.drivers.single().name)
            assertEquals("Fixture Team", data.championship.data.constructors.single().name)
            assertEquals(
                "one",
                data.results["2026/1/RACE"]?.data?.single()?.entries?.single()?.driverId,
            )
            assertFalse(data.calendar.stale)
            assertEquals(0, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun expiredDataIsRestoredWithItsOriginalTimestampAndStaleFlag() = runTest {
        val saved = clock.millis() - 7 * 3600_000L
        val cache = MemoryCache().apply { seed(saved) }
        val client = HttpClient(MockEngine { error("offline") })
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.restoreHomeCache()
            assertEquals(saved, repo.data.value.calendar.updatedAt)
            assertTrue(repo.data.value.calendar.stale)
            assertTrue(repo.data.value.championship.data.driversInfo.stale)
            assertTrue(repo.data.value.results["2026/1/RACE"]!!.stale)
            assertEquals(1, repo.data.value.calendar.data.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun anOlderDiskSnapshotDoesNotReplaceNewerInMemoryData() = runTest {
        val cache = MemoryCache().apply { seed(clock.millis()) }
        val client = HttpClient(MockEngine { error("must not fetch") })
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.restoreHomeCache()
            cache.write(
                "2026/races",
                listOf(
                    Json.parseToJsonElement(calendar.replace("Fixture GP", "Old GP")).jsonObject
                ),
                clock.millis() - 86400_000,
            )
            repo.restoreHomeCache()
            assertEquals("Fixture GP", repo.data.value.calendar.data.single().raceName)
            assertEquals(clock.millis(), repo.data.value.calendar.updatedAt)
        } finally {
            client.close()
        }
    }

    @Test
    fun mismatchedCachedSeasonIsNotAssignedToTheActiveChampionship() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2026/driverstandings",
                    listOf(
                        Json.parseToJsonElement(
                                """{"season":"2025","round":"1","DriverStandings":[{"Driver":{"driverId":"old"}}]}"""
                            )
                            .jsonObject
                    ),
                    clock.millis(),
                )
            }
        val client = HttpClient(MockEngine { error("must not fetch") })
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.restoreHomeCache()
            assertTrue(repo.data.value.championship.data.drivers.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun previousSeasonRoundCacheRestoresTheLastRaceBeforeTheFirstCurrentRace() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2025/races",
                    listOf(Json.parseToJsonElement(calendar.replace("2026", "2025")).jsonObject),
                    clock.millis(),
                )
                write(
                    "2025/1/results",
                    listOf(
                        Json.parseToJsonElement(
                                """{"season":"2025","round":"1","raceName":"Previous Fixture GP","date":"2025-12-01","Results":[{"position":"1","Driver":{"driverId":"one"}}]}"""
                            )
                            .jsonObject
                    ),
                    clock.millis(),
                )
            }
        val client = HttpClient(MockEngine { error("must not fetch") })
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.restoreHomeCache()
            assertEquals("2025", repo.data.value.results["2025/1/RACE"]?.data?.single()?.season)
            assertEquals(2026, repo.data.value.year)
            assertTrue(repo.data.value.calendar.data.isEmpty())
        } finally {
            client.close()
        }
    }
}
