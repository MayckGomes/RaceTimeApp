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
import mayckgomes.com.racetimeapp.domain.weekend.SessionType
import org.junit.Assert.*
import org.junit.Test

class StatisticsRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC)

    private class MemoryCache : ResponseCache {
        private val values = java.util.concurrent.ConcurrentHashMap<String, StoredResponse>()

        override fun read(key: String) = values[key]

        override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
            values[key] = StoredResponse(rows, savedAt)
        }
    }

    private fun race(fields: String, year: Int = 2026, round: Int = 1) =
        """{"season":"$year","round":"$round","raceName":"Fixture GP","date":"$year-04-01",$fields}"""

    private fun page(row: String = "", total: Int = 1, limit: Int = 100) =
        """{"MRData":{"total":"$total","limit":"$limit","RaceTable":{"Races":[$row]}}}"""

    private fun pit(stop: Int, duration: String = "23.456") =
        """{"driverId":"one","stop":"$stop","lap":"${stop * 10}","duration":"$duration","time":"14:20:00"}"""

    private fun lap(number: Int, id: String = "one", time: String = "1:23.456") =
        """{"number":"$number","Timings":[{"driverId":"$id","position":"2","time":"$time"}]}"""

    @Test
    fun pitStopsFollowPaginationAndKeepTheirReportedDuration() = runTest {
        val offsets = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { req ->
                    val offset = req.url.parameters["offset"] ?: "0"
                    offsets += offset
                    respond(page(race("\"PitStops\":[${pit(if (offset == "0") 1 else 2)}]"), 2, 1))
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .pitStops(2026, "1")
            assertEquals(listOf("0", "1"), offsets)
            assertEquals(listOf("1", "2"), result.data.map { it.stop })
            assertEquals("23.456", result.data.first().duration)
            assertFalse(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun driverLapsUseTheDocumentedFilteredRouteAndMergePagesInLapOrder() = runTest {
        val paths = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { req ->
                    paths += req.url.encodedPath
                    val second = req.url.parameters["offset"] == "1"
                    respond(page(race("\"Laps\":[${lap(if (second) 1 else 2)}]"), 2, 1))
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .lapTimes(2026, "1", "one")
            assertEquals(listOf(1, 2), result.data.map { it.lap })
            assertFalse(result.failed)
            assertTrue(paths.all { it.endsWith("2026/1/drivers/one/laps.json") })
        } finally {
            client.close()
        }
    }

    @Test
    fun unsupportedHistoricalYearsDoNotIssueRequests() = runTest {
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    error("must not fetch")
                }
            )
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            assertTrue(repo.pitStops(2010, "1").data.isEmpty())
            assertTrue(repo.lapTimes(1995, "1", "one").data.isEmpty())
            assertEquals(0, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun wrongGpOrDriverIsRejectedInsteadOfLeakingAnotherSelection() = runTest {
        val client =
            HttpClient(
                MockEngine { req ->
                    respond(
                        if (req.url.encodedPath.contains("pitstops"))
                            page(race("\"PitStops\":[${pit(1)}]", round = 2))
                        else page(race("\"Laps\":[${lap(1, "other")}]"))
                    )
                }
            )
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            val pits = repo.pitStops(2026, "1")
            assertTrue(pits.failed)
            assertTrue(pits.data.isEmpty())
            val laps = repo.lapTimes(2026, "1", "one")
            assertTrue(laps.failed)
            assertTrue(laps.data.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun successfulEmptyResponseIsDifferentFromAFailedRequest() = runTest {
        val client =
            HttpClient(
                MockEngine { req ->
                    if (req.url.encodedPath.contains("pitstops")) respond(page(total = 0))
                    else respond("{}", HttpStatusCode.NotFound)
                }
            )
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            assertFalse(repo.pitStops(2026, "1").failed)
            assertTrue(repo.lapTimes(2026, "1", "one").failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun cachedLapTimesRemainAvailableOnNetworkFailure() = runTest {
        val saved = clock.millis() - 86400_000
        val cache =
            MemoryCache().apply {
                write(
                    "2026/1/drivers/one/laps",
                    listOf(Json.parseToJsonElement(race("\"Laps\":[${lap(1)}]")).jsonObject),
                    saved,
                )
            }
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.NotFound) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock)
                    .lapTimes(2026, "1", "one")
            assertEquals(1, result.data.single().lap)
            assertTrue(result.stale)
            assertTrue(result.failed)
            assertEquals(saved, result.updatedAt)
        } finally {
            client.close()
        }
    }

    @Test
    fun optionalFieldsAreNotReplacedByInventedValues() = runTest {
        val client =
            HttpClient(MockEngine { respond(page(race(""""PitStops":[{"driverId":"one"}]"""))) })
        try {
            val stop =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .pitStops(2026, "1")
                    .data
                    .single()
            assertEquals("", stop.duration)
            assertEquals("", stop.lap)
            assertEquals("", stop.stop)
        } finally {
            client.close()
        }
    }

    @Test
    fun conflictingLapRecordsAreOmittedAndReportedAsFailure() = runTest {
        val client =
            HttpClient(
                MockEngine {
                    respond(page(race("\"Laps\":[${lap(1)},${lap(1, time = "1:24.000")}]")))
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .lapTimes(2026, "1", "one")
            assertTrue(result.data.isEmpty())
            assertTrue(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun repeatedIdenticalLapRecordsDoNotDuplicateRows() = runTest {
        val client =
            HttpClient(MockEngine { respond(page(race("\"Laps\":[${lap(1)},${lap(1)}]"))) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .lapTimes(2026, "1", "one")
            assertEquals(1, result.data.size)
            assertFalse(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun resultMappingKeepsFastestLapNumberAndCompletedLaps() = runTest {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        page(
                            race(
                                """"Results":[{"position":"1","laps":"58","Driver":{"driverId":"one"},"FastestLap":{"rank":"1","lap":"42","Time":{"time":"1:23.456"}}}]"""
                            )
                        )
                    )
                }
            )
        try {
            val entry =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .result("2026", "1", SessionType.RACE)
                    .data
                    .single()
                    .entries
                    .single()
            assertEquals("42", entry.fastestLap)
            assertEquals("58", entry.completedLaps)
            assertEquals("1:23.456", entry.fastestTime)
        } finally {
            client.close()
        }
    }

    @Test
    fun populatedHistoricalDataUsesTheLongCacheWithoutANetworkRequest() = runTest {
        val saved = clock.millis() - 3 * 86400_000L
        val cache =
            MemoryCache().apply {
                write(
                    "2025/1/pitstops",
                    listOf(
                        Json.parseToJsonElement(race("\"PitStops\":[${pit(1)}]", year = 2025))
                            .jsonObject
                    ),
                    saved,
                )
            }
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    error("must not fetch")
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock).pitStops(2025, "1")
            assertEquals(1, result.data.size)
            assertEquals(0, calls)
            assertEquals(saved, result.updatedAt)
        } finally {
            client.close()
        }
    }

    @Test
    fun historicalEmptyCacheCanBeRevalidatedSoonerThanPublishedData() = runTest {
        val cache =
            MemoryCache().apply {
                write("2025/1/pitstops", emptyList(), clock.millis() - 6 * 60_000L)
            }
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    respond(page(race("\"PitStops\":[${pit(1)}]", year = 2025)))
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock).pitStops(2025, "1")
            assertEquals(1, result.data.size)
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }
}
