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

class FavoriteRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC)

    private class MemoryCache : ResponseCache {
        private val values = java.util.concurrent.ConcurrentHashMap<String, StoredResponse>()

        override fun read(key: String) = values[key]

        override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
            values[key] = StoredResponse(rows, savedAt)
        }
    }

    private fun seasons(vararg years: Int) =
        """{"MRData":{"total":"${years.size}","limit":"100","SeasonTable":{"Seasons":[${years.joinToString(",") { """{"season":"$it"}""" }}]}}}"""

    private fun entry(driver: String, constructor: String, pos: Int) =
        """{"position":"$pos","status":"Engine","Driver":{"driverId":"$driver","givenName":"Fixture","familyName":"Driver"},"Constructor":{"constructorId":"$constructor","name":"Fixture Team"}}"""

    private fun race(year: Int, round: Int, entries: String) =
        """{"season":"$year","round":"$round","raceName":"Fixture GP","date":"$year-04-01","Results":[$entries]}"""

    private fun races(rows: String, total: Int = 1, limit: Int = 100) =
        """{"MRData":{"total":"$total","limit":"$limit","RaceTable":{"Races":[$rows]}}}"""

    @Test
    fun currentFavoriteUsesTheActiveSeasonAndKeepsPartialResultsOutOfTheDashboard() = runTest {
        val paths = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { req ->
                    paths += req.url.encodedPath
                    respond(
                        when {
                            req.url.encodedPath.contains("driverstandings") ->
                                """{"MRData":{"total":"1","StandingsTable":{"StandingsLists":[{"season":"2026","round":"2","DriverStandings":[{"position":"1","Driver":{"driverId":"active","givenName":"Fixture","familyName":"Driver"}}]}]}}}"""
                            req.url.encodedPath.contains("constructorstandings") ->
                                """{"MRData":{"total":"0","StandingsTable":{"StandingsLists":[]}}}"""
                            else -> races(race(2026, 2, entry("active", "team", 1)))
                        }
                    )
                }
            )
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship(includeHistory = false)
            val result = repo.favoriteRace("active", false)
            assertEquals("2026", result.data?.season)
            assertTrue(paths.any { it.contains("2026/drivers/active/results") })
            assertFalse(paths.any { it.contains("seasons") })
            assertTrue(repo.data.value.results.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun optionalProfileNumberDoesNotPreventAnOldDriverFromLoading() = runTest {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        """{"MRData":{"total":"1","DriverTable":{"Drivers":[{"driverId":"old","givenName":"Fixture","familyName":"Driver"}]}}}"""
                    )
                }
            )
        try {
            val profile =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .favoriteProfile("old", false)
            assertEquals("Fixture Driver", profile.data?.name)
            assertEquals("", profile.data?.number)
            assertFalse(profile.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun wrongStableProfileIdIsRejected() = runTest {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        """{"MRData":{"total":"1","ConstructorTable":{"Constructors":[{"constructorId":"other","name":"Other"}]}}}"""
                    )
                }
            )
        try {
            val profile =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .favoriteProfile("saved", true)
            assertNull(profile.data)
            assertTrue(profile.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun retiredFavoriteUsesSourceSeasonsAndDoesNotPolluteGlobalGpResults() = runTest {
        val paths = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { req ->
                    paths += req.url.encodedPath
                    respond(
                        if (req.url.encodedPath.contains("seasons")) seasons(2020, 2024)
                        else races(race(2024, 2, entry("old", "team", 7)))
                    )
                }
            )
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            val result = repo.favoriteRace("old", false)
            assertEquals("2024", result.data?.season)
            assertEquals("7", result.data?.entries?.single()?.position)
            assertTrue(paths.any { it.contains("2024/drivers/old/results") })
            assertTrue(repo.data.value.results.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun pagesOfConstructorResultsMergeBeforeChoosingLatestGp() = runTest {
        val client =
            HttpClient(
                MockEngine { req ->
                    respond(
                        if (req.url.encodedPath.contains("seasons")) seasons(2025)
                        else {
                            val second = req.url.parameters["offset"] == "1"
                            races(
                                race(
                                    2025,
                                    3,
                                    entry(
                                        if (second) "two" else "one",
                                        "team",
                                        if (second) 4 else 2,
                                    ),
                                ),
                                2,
                                1,
                            )
                        }
                    )
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .favoriteRace("team", true)
            assertEquals(listOf("one", "two"), result.data?.entries?.map { it.driverId })
            assertFalse(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun historicalSearchIsBoundedAndEmptyIsDifferentFromFailure() = runTest {
        val paths = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { req ->
                    paths += req.url.encodedPath
                    respond(
                        if (req.url.encodedPath.contains("seasons")) seasons(2022, 2023, 2024)
                        else races("", 0)
                    )
                }
            )
        try {
            val result =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .favoriteRace("old", false)
            assertNull(result.data)
            assertFalse(result.failed)
            assertEquals(3, paths.size)
            assertFalse(paths.any { it.contains("2022/") })
        } finally {
            client.close()
        }
    }

    @Test
    fun historicalResultKeepsItsOwnCacheTimestamp() = runTest {
        val saved = clock.millis() - 3600_000
        val cache =
            MemoryCache().apply {
                write(
                    "2025/drivers/old/results",
                    listOf(
                        Json.parseToJsonElement(race(2025, 2, entry("old", "team", 4))).jsonObject
                    ),
                    saved,
                )
            }
        val client = HttpClient(MockEngine { respond(seasons(2025)) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock)
                    .favoriteRace("old", false)
            assertEquals(saved, result.updatedAt)
            assertNotNull(result.data)
        } finally {
            client.close()
        }
    }

    @Test
    fun staleProfileIsRetainedOnNetworkFailure() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "drivers/old",
                    listOf(
                        Json.parseToJsonElement(
                                """{"driverId":"old","givenName":"Saved","familyName":"Profile"}"""
                            )
                            .jsonObject
                    ),
                    clock.millis() - 9 * 86400_000L,
                )
            }
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.NotFound) })
        try {
            val result =
                RacingRepository(JolpicaGateway(client, cache, clock), clock)
                    .favoriteProfile("old", false)
            assertEquals("Saved Profile", result.data?.name)
            assertTrue(result.stale)
            assertTrue(result.failed)
        } finally {
            client.close()
        }
    }

    @Test
    fun driverProfileKeepsBirthDateCodeAndCleanFullName() = runTest {
        val client =
            HttpClient(
                MockEngine {
                    respond(
                        """{"MRData":{"total":"1","DriverTable":{"Drivers":[{"driverId":"one","givenName":" Fixture ","familyName":" Driver ","dateOfBirth":"2000-01-01","code":"FIX","permanentNumber":"44"}]}}}"""
                    )
                }
            )
        try {
            val profile =
                RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
                    .favoriteProfile("one", false)
                    .data
            assertEquals("Fixture Driver", profile?.name)
            assertEquals("2000-01-01", profile?.dateOfBirth)
            assertEquals("FIX", profile?.code)
            assertEquals("44", profile?.number)
        } finally {
            client.close()
        }
    }
}
