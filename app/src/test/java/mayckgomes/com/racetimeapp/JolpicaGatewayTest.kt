package mayckgomes.com.racetimeapp

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.time.*
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.local.*
import mayckgomes.com.racetimeapp.data.remote.*
import mayckgomes.com.racetimeapp.data.repository.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import org.junit.Assert.*
import org.junit.Test

class JolpicaGatewayTest {
    private val clock = Clock.fixed(Instant.parse("2026-04-10T12:00:00Z"), ZoneOffset.UTC)

    private class MemoryCache : ResponseCache {
        val values = mutableMapOf<String, StoredResponse>()

        override fun read(key: String) = values[key]

        override fun write(key: String, rows: List<JsonObject>, savedAt: Long) {
            values[key] = StoredResponse(rows, savedAt)
        }
    }

    private fun page(rows: String, total: Int = 0, limit: Int = 100) =
        """{"MRData":{"limit":"$limit","total":"$total","RaceTable":{"Races":$rows}}}"""

    @Test
    fun paginationUsesReportedLimitAndTotal() = runTest {
        val offsets = mutableListOf<String>()
        val client =
            HttpClient(
                MockEngine { request ->
                    val offset = request.url.parameters["offset"] ?: "0"
                    offsets += offset
                    val end = minOf(offset.toInt() + 2, 5)
                    val rows =
                        (offset.toInt() until end).joinToString(",") { """{"round":"${it + 1}"}""" }
                    respond(
                        page("[$rows]", 5, 2),
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            )
        try {
            val gateway = JolpicaGateway(client, MemoryCache(), clock)
            assertEquals(5, gateway.query("2026/races", "RaceTable.Races", 1000).rows.size)
            assertEquals(listOf("0", "2", "4"), offsets)
        } finally {
            client.close()
        }
    }

    @Test
    fun emptySuccessIsNotAnError() = runTest {
        val client = HttpClient(MockEngine { respond(page("[]")) })
        try {
            val result =
                JolpicaGateway(client, MemoryCache(), clock)
                    .query("2026/races", "RaceTable.Races", 1000)
            assertTrue(result.rows.isEmpty())
            assertFalse(result.failed)
            assertFalse(result.stale)
        } finally {
            client.close()
        }
    }

    @Test
    fun simultaneousRequestsShareOneNetworkResponse() = runTest {
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    respond(page("[]"))
                }
            )
        try {
            val gateway = JolpicaGateway(client, MemoryCache(), clock)
            val a = async { gateway.query("2026/races", "RaceTable.Races", 1000, true) }
            val b = async { gateway.query("2026/races", "RaceTable.Races", 1000, true) }
            a.await()
            b.await()
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun networkErrorFallsBackToStaleCache() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2026/races",
                    listOf(buildJsonObject { put("round", "1") }),
                    clock.millis() - 3600_000,
                )
            }
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    error("offline")
                }
            )
        try {
            val result =
                JolpicaGateway(client, cache, clock).query("2026/races", "RaceTable.Races", 1000)
            assertTrue(result.failed)
            assertTrue(result.stale)
            assertEquals(1, result.rows.size)
            assertEquals(3, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun http429StopsRetriesAndAppliesGlobalCooldown() = runTest {
        var calls = 0
        val client =
            HttpClient(
                MockEngine {
                    calls++
                    respond("{}", HttpStatusCode.TooManyRequests, headersOf("Retry-After", "60"))
                }
            )
        try {
            val gateway = JolpicaGateway(client, MemoryCache(), clock)
            assertTrue(gateway.query("2026/races", "RaceTable.Races", 1000).failed)
            assertTrue(
                gateway.query("2026/driverstandings", "StandingsTable.StandingsLists", 1000).failed
            )
            assertEquals(1, calls)
        } finally {
            client.close()
        }
    }

    @Test
    fun currentResultAcceptsMissingOptionalConstructorAndStatus() = runTest {
        val fixture =
            """[{"season":"2026","round":"1","raceName":"Fixture GP","date":"2026-04-09","Results":[{"position":"1","Driver":{"driverId":"stable_id","givenName":"Fixture","familyName":"Driver"},"Constructor":null,"status":null}]}]"""
        val client = HttpClient(MockEngine { respond(page(fixture, 1)) })
        try {
            val repo = RacingRepository(JolpicaGateway(client, MemoryCache(), clock), clock)
            val result = repo.result("2026", "1", SessionType.RACE)
            assertFalse(result.failed)
            assertEquals("stable_id", result.data.single().entries.single().driverId)
            assertEquals("", result.data.single().entries.single().status)
            assertEquals("", result.data.single().entries.single().team)
        } finally {
            client.close()
        }
    }

    @Test
    fun partialPaginationDoesNotReplaceACompleteCache() = runTest {
        val cache =
            MemoryCache().apply {
                write(
                    "2026/races",
                    listOf(buildJsonObject { put("round", "saved") }),
                    clock.millis() - 3600_000,
                )
            }
        val client =
            HttpClient(
                MockEngine { request ->
                    if (request.url.parameters["offset"] == "0")
                        respond(page("[{\"round\":\"1\"}]", 2, 1))
                    else error("offline")
                }
            )
        try {
            val result =
                JolpicaGateway(client, cache, clock).query("2026/races", "RaceTable.Races", 1)
            assertTrue(result.failed)
            assertEquals("saved", result.rows.single().text("round"))
            assertEquals("saved", cache.read("2026/races")!!.rows.single().text("round"))
        } finally {
            client.close()
        }
    }
}
