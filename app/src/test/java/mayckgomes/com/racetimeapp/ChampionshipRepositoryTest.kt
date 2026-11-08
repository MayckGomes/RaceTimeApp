package mayckgomes.com.racetimeapp

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.time.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.local.*
import mayckgomes.com.racetimeapp.data.remote.*
import mayckgomes.com.racetimeapp.data.repository.RacingRepository
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class ChampionshipRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-30T12:00:00Z"), ZoneOffset.UTC)

    private class MemoryCache : ResponseCache {
        private val values = java.util.concurrent.ConcurrentHashMap<String, StoredResponse>()

        override fun read(key: String) = values[key]

        override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
            values[key] = StoredResponse(rows, savedAt)
        }
    }

    private fun standings(
        round: String,
        drivers: Boolean,
        position: String = "2",
        season: String = "2026",
    ): String {
        val person =
            if (drivers)
                """"Driver":{"driverId":"driver","givenName":"Fixture","familyName":"Driver"},"Constructors":[{"constructorId":"team","name":"Fixture Team"}]"""
            else """"Constructor":{"constructorId":"team","name":"Fixture Team"}"""
        val field = if (drivers) "DriverStandings" else "ConstructorStandings"
        return """{"MRData":{"limit":"100","total":"1","StandingsTable":{"StandingsLists":[{"season":"$season","round":"$round","$field":[{"position":"$position","points":"100","wins":"2",$person}]}]}}}"""
    }

    private fun race(round: String, published: Boolean = true): String {
        val rows =
            if (published)
                """[{"season":"2026","round":"$round","raceName":"Fixture GP","date":"2026-04-09","Results":[{"position":"1","Driver":{"driverId":"driver","givenName":"Fixture","familyName":"Driver"}}]}]"""
            else "[]"
        return """{"MRData":{"limit":"100","total":"${if (published) 1 else 0}","RaceTable":{"Races":$rows}}}"""
    }

    private fun responses(): MutableMap<String, String> =
        mutableMapOf(
            "2026/driverstandings" to standings("3", true),
            "2026/constructorstandings" to standings("3", false),
            "2026/3/results" to race("3"),
            "2026/2/results" to race("2"),
            "2026/2/driverstandings" to standings("2", true, "4"),
            "2026/2/constructorstandings" to standings("2", false, "1"),
        )

    private fun client(responses: Map<String, String>, calls: MutableList<String>) =
        HttpClient(
            MockEngine { request ->
                val path =
                    request.url.encodedPath.substringAfter("/ergast/f1/").removeSuffix(".json")
                calls.add(path)
                respond(
                    responses[path] ?: race("0", false),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        )

    private fun calls(): MutableList<String> =
        java.util.Collections.synchronizedList(mutableListOf())

    @Test
    fun onlyTwoConfirmedRoundsEnableComparison() = runTest {
        val calls = calls()
        val client = client(responses(), calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship()
            val state = repo.data.value.championship.data
            assertTrue(state.driversInfo.comparisonReady)
            assertTrue(state.constructorsInfo.comparisonReady)
            assertEquals(
                2,
                confirmedMovement(state.drivers.single(), state.previousDrivers, state.driversInfo),
            )
            assertEquals(
                -1,
                confirmedMovement(
                    state.constructors.single(),
                    state.previousConstructors,
                    state.constructorsInfo,
                ),
            )
            assertEquals("2", state.drivers.single().wins)
            assertEquals(1, calls.count { it == "2026/3/results" })
        } finally {
            client.close()
        }
    }

    @Test
    fun firstRoundNeedsNoHistoryRequests() = runTest {
        val responses =
            responses().apply {
                put("2026/driverstandings", standings("1", true))
                put("2026/constructorstandings", standings("1", false))
            }
        val calls = calls()
        val client = client(responses, calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship()
            assertFalse(repo.data.value.championship.data.driversInfo.comparisonReady)
            assertEquals(2, calls.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun sprintPointsWithoutRaceResultsDoNotCreateAMovement() = runTest {
        val responses = responses().apply { put("2026/3/results", race("3", false)) }
        val calls = calls()
        val client = client(responses, calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship()
            val state = repo.data.value.championship.data
            assertFalse(state.driversInfo.comparisonReady)
            assertFalse(state.constructorsInfo.comparisonReady)
            assertEquals("100", state.drivers.single().points)
            assertFalse(calls.any { it == "2026/2/driverstandings" })
        } finally {
            client.close()
        }
    }

    @Test
    fun unavailablePreviousRaceDoesNotInventCancellationOrMovement() = runTest {
        val responses = responses().apply { put("2026/2/results", race("2", false)) }
        val calls = calls()
        val client = client(responses, calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship()
            assertTrue(repo.data.value.championship.data.previousDrivers.isEmpty())
            assertFalse(repo.data.value.championship.data.driversInfo.comparisonReady)
        } finally {
            client.close()
        }
    }

    @Test
    fun driversAndConstructorsUseTheirOwnPublishedRounds() = runTest {
        val responses =
            responses().apply {
                put("2026/constructorstandings", standings("2", false))
                put("2026/1/results", race("1"))
                put("2026/1/constructorstandings", standings("1", false, "3"))
            }
        val calls = calls()
        val client = client(responses, calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship()
            val state = repo.data.value.championship.data
            assertEquals("3", state.driversInfo.round)
            assertEquals("2", state.driversInfo.previousRound)
            assertEquals("2", state.constructorsInfo.round)
            assertEquals("1", state.constructorsInfo.previousRound)
            assertEquals(
                1,
                confirmedMovement(
                    state.constructors.single(),
                    state.previousConstructors,
                    state.constructorsInfo,
                ),
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun mismatchedHistoricalMetadataIsRejectedIndependently() = runTest {
        val responses =
            responses().apply { put("2026/2/driverstandings", standings("1", true, "4", "2025")) }
        val calls = calls()
        val client = client(responses, calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship()
            val state = repo.data.value.championship.data
            assertFalse(state.driversInfo.comparisonReady)
            assertTrue(state.constructorsInfo.comparisonReady)
        } finally {
            client.close()
        }
    }

    @Test
    fun homeDoesNotFetchComparisonBeforeChampionshipIsOpened() = runTest {
        val calls = calls()
        val client = client(responses(), calls)
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            repo.refreshChampionship(includeHistory = false)
            assertEquals(2, calls.size)
            assertFalse(repo.data.value.championship.data.driversInfo.comparisonReady)
        } finally {
            client.close()
        }
    }

    @Test
    fun offlineHistoryCanUseACompleteSavedSnapshotWithAStaleNotice() = runTest {
        val responses = responses()
        val cache = MemoryCache()
        val historical =
            Json.parseToJsonElement(responses.getValue("2026/2/driverstandings"))
                .jsonObject
                .getValue("MRData")
                .jsonObject
                .getValue("StandingsTable")
                .jsonObject
                .getValue("StandingsLists")
                .jsonArray
                .map { it.jsonObject }
        cache.write("2026/2/driverstandings", historical, clock.millis() - 2 * 24 * 3600_000L)
        val client =
            HttpClient(
                MockEngine { request ->
                    val path =
                        request.url.encodedPath.substringAfter("/ergast/f1/").removeSuffix(".json")
                    if (path == "2026/2/driverstandings")
                        respond("{}", HttpStatusCode.ServiceUnavailable)
                    else
                        respond(
                            responses[path] ?: race("0", false),
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                }
            )
        try {
            val repo = RacingRepository(JolpicaGateway(client, cache, clock), clock)
            repo.refreshChampionship()
            val state = repo.data.value.championship.data
            assertTrue(state.driversInfo.comparisonReady)
            assertTrue(state.driversInfo.historyStale)
            assertEquals(
                2,
                confirmedMovement(state.drivers.single(), state.previousDrivers, state.driversInfo),
            )
        } finally {
            client.close()
        }
    }
}
