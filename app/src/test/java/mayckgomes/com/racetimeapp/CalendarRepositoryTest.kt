package mayckgomes.com.racetimeapp

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.time.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.local.*
import mayckgomes.com.racetimeapp.data.remote.JolpicaGateway
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import org.junit.Assert.*
import org.junit.Test

class CalendarRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC)

    private class MemoryCache : ResponseCache {
        private val values = java.util.concurrent.ConcurrentHashMap<String, StoredResponse>()

        override fun read(key: String) = values[key]

        override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
            values[key] = StoredResponse(rows, savedAt)
        }
    }

    private fun race(round: Int = 1, season: Int = 2026): String =
        """{"season":"$season","round":"$round","raceName":"Fixture GP","date":"$season-04-09","time":"14:00:00Z","Circuit":{"circuitId":"fixture","circuitName":"Fixture Circuit","url":"https://en.wikipedia.org/wiki/Fixture","Location":{"country":"Fixture Country","locality":"Fixture City","lat":"1.0","long":"2.0"}}}"""

    private fun page(rows: String, total: Int = 1, limit: Int = 100): String =
        """{"MRData":{"total":"$total","limit":"$limit","RaceTable":{"Races":$rows}}}"""

    @Test
    fun seasonsFollowPaginationAndAreReturnedNewestFirst() = runTest {
        val offsets = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { req ->
                    val offset = req.url.parameters["offset"] ?: "0"
                    offsets += offset
                    val rows =
                        if (offset == "0") """[{"season":"1950"},{"season":"2026"}]"""
                        else """[{"season":"2027"}]"""
                    respond(
                        """{"MRData":{"total":"3","limit":"2","SeasonTable":{"Seasons":$rows}}}"""
                    )
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock).seasons()
            assertEquals(listOf(2027, 2026, 1950), result.data)
            assertEquals(listOf("0", "2"), offsets)
        } finally {
            client.close()
        }
    }

    @Test
    fun aCalendarCanSpanMoreThanOnePage() = runTest {
        val client =
            HttpClient(
                MockEngine { req ->
                    val offset = req.url.parameters["offset"]?.toInt() ?: 0
                    val rows =
                        (offset until minOf(offset + 2, 5)).joinToString(",") { race(it + 1) }
                    respond(page("[$rows]", 5, 2))
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock).calendar(2026)
            assertEquals(listOf("1", "2", "3", "4", "5"), result.data.map { it.round })
            assertFalse(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun freshSeasonCacheAvoidsAnExtraMetadataRequestForTheGp() = runTest {
        var calls = 0
        val cache =
            MemoryCache().apply {
                write(
                    "2026/races",
                    listOf(Json.parseToJsonElement(race()).jsonObject),
                    clock.millis(),
                )
            }
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    error("must not fetch")
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock).weekend(2026, "1")
            assertEquals("Fixture GP", result.data?.raceName)
            assertEquals(0, calls)
            assertEquals("1.0", result.data?.Circuit?.Location?.lat)
        } finally {
            client.close()
        }
    }

    @Test
    fun staleSeasonCacheIsRetainedIfTheGpRequestFails() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2026/races",
                    listOf(Json.parseToJsonElement(race()).jsonObject),
                    clock.millis() - 86400_000,
                )
            }
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.NotFound) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock).weekend(2026, "1")
            assertTrue(result.failed)
            assertTrue(result.stale)
            assertEquals("1", result.data?.round)
        } finally {
            client.close()
        }
    }

    @Test
    fun aConfirmedEmptyMetadataResponseDoesNotReintroduceARemovedEvent() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2026/races",
                    listOf(Json.parseToJsonElement(race()).jsonObject),
                    clock.millis(),
                )
            }
        val client = HttpClient(MockEngine { respond(page("[]", 0)) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock)
                    .weekend(2026, "1", true)
            assertNull(result.data)
            assertFalse(result.failed)
            assertTrue(cache.read("2026/races")!!.rows.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun mismatchedEventMetadataIsARejectedResponseRatherThanAnotherGp() = runTest {
        val client = HttpClient(MockEngine { respond(page("[${race(2)}]")) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .weekend(2026, "1")
            assertNull(result.data)
            assertTrue(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun selectingAHistoricalCalendarDoesNotChangeTheActiveSeason() = runTest {
        val client = HttpClient(MockEngine { respond(page("[${race(1, 2025)}]")) })
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            assertEquals("2025", repo.calendar(2025).data.single().season)
            assertEquals(2026, repo.data.value.year)
        } finally {
            client.close()
        }
    }

    @Test
    fun refreshingOneGpUpdatesItsCachedScheduleWithoutRefreshingTheWholeSeasonTimestamp() =
        runTest {
            val savedAt = clock.millis() - 1000
            val cache =
                MemoryCache().apply {
                    write("2026/races", listOf(Json.parseToJsonElement(race()).jsonObject), savedAt)
                }
            var calls = 0
            val client =
                HttpClient(
                    MockEngine {
                        calls++
                        respond(page("[${race().replace("14:00:00Z", "16:00:00Z")}]"))
                    }
                )
            try {
                val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
                repo.weekend(2026, "1", true)
                val calendar = repo.calendar(2026)
                assertEquals("16:00:00Z", calendar.data.single().time)
                assertEquals(savedAt, cache.read("2026/races")?.savedAt)
                assertEquals(1, calls)
            } finally {
                client.close()
            }
        }

    @Test
    fun aNewConfirmedGpCanBeMergedIntoAnExistingCalendarWithoutInventingOtherRounds() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2026/races",
                    listOf(Json.parseToJsonElement(race()).jsonObject),
                    clock.millis() - 1000,
                )
            }
        val client = HttpClient(MockEngine { respond(page("[${race(2)}]")) })
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.weekend(2026, "2")
            assertEquals(listOf("1", "2"), repo.calendar(2026).data.map { it.round })
        } finally {
            client.close()
        }
    }
}
