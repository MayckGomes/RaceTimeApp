package mayckgomes.com.racetimeapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.local.ResponseCache

// Toda consulta nova e a fachada legada compartilham cache, paginação e limite de rede.
data class CachedJson(
    val rows: List<JsonObject>,
    val stale: Boolean,
    val failed: Boolean,
    val updatedAt: Long,
)

class JolpicaGateway(
    private val client: HttpClient,
    private val cache: ResponseCache,
    private val clock: Clock,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val network = Mutex()
    private var lastCall = 0L
    @Volatile
    private var cooldownUntil =
        cache.read("@cooldown")?.rows?.firstOrNull()?.text("until")?.toLongOrNull() ?: 0L
    private val recentCalls =
        java.util.ArrayDeque<Long>().apply {
            cache.read("@rate_budget")?.rows?.forEach { row ->
                row.text("at").toLongOrNull()?.let { addLast(it) }
            }
        }

    private class NonRetryable(message: String) : Exception(message)

    private val attempts = ConcurrentHashMap<String, Long>()

    fun cached(route: String, ttl: Long): CachedJson? =
        cache.read(route)?.let {
            val age = clock.millis() - it.savedAt
            CachedJson(
                it.rows,
                stale = age < 0 || age >= ttl,
                failed = false,
                updatedAt = it.savedAt,
            )
        }

    suspend fun patchCached(
        route: String,
        sourceUpdatedAt: Long,
        transform: (List<JsonObject>) -> List<JsonObject>,
    ) =
        locks
            .getOrPut(route) { Mutex() }
            .withLock {
                val saved = cache.read(route) ?: return@withLock
                if (saved.savedAt <= sourceUpdatedAt)
                    cache.write(route, transform(saved.rows), saved.savedAt)
            }

    suspend fun query(
        route: String,
        responsePath: String,
        ttl: Long,
        force: Boolean = false,
    ): CachedJson =
        locks
            .getOrPut(route) { Mutex() }
            .withLock {
                val saved = cache.read(route)
                val cached = saved?.rows
                val cachedAt = saved?.savedAt ?: 0L
                val now = clock.millis()
                val lastAttempt = attempts[route] ?: 0
                val age = now - cachedAt
                if (cached != null && age >= 0 && ((!force && age < ttl) || age < 20_000))
                    return@withLock CachedJson(cached, false, false, cachedAt)
                if (now - lastAttempt in 0L until 20_000L)
                    return@withLock CachedJson(cached.orEmpty(), true, true, cachedAt)
                attempts[route] = now
                try {
                    val responseRows = mutableListOf<JsonObject>()
                    var offset = 0
                    do {
                        val response = request(route, offset)
                        val responseMetadata = response["MRData"]?.jsonObject ?: error("MRData")
                        var responseNodes: List<JsonElement> = listOf(responseMetadata)
                        responsePath.split('.').forEach { pathSegment ->
                            responseNodes =
                                responseNodes.flatMap { responseNode ->
                                    when (
                                        val fieldValue =
                                            (responseNode as? JsonObject)?.get(pathSegment)
                                    ) {
                                        is JsonArray -> fieldValue.toList()
                                        null,
                                        JsonNull -> error("Missing response field: $pathSegment")
                                        else -> listOf(fieldValue)
                                    }
                                }
                        }
                        responseRows += responseNodes.mapNotNull { it as? JsonObject }
                        val limit =
                            responseMetadata.text("limit").toIntOrNull()?.takeIf { it > 0 } ?: 100
                        val total =
                            responseMetadata.text("total").toIntOrNull() ?: responseRows.size
                        check(total == 0 || responseNodes.isNotEmpty()) { "Incomplete pagination" }
                        if (offset + limit >= total) break
                        offset += limit
                    } while (true)
                    val timestamp = clock.millis()
                    cache.write(route, responseRows, timestamp)
                    CachedJson(responseRows, false, false, timestamp)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    CachedJson(cached.orEmpty(), true, true, cachedAt)
                }
            }

    private suspend fun request(route: String, offset: Int): JsonObject {
        repeat(3) { attemptIndex ->
            try {
                val response =
                    network.withLock {
                        val now = clock.millis()
                        if (cooldownUntil > now) throw IllegalStateException("API cooldown")
                        while (
                            recentCalls.peekFirst()?.let { now - it >= 3600_000 || it > now } ==
                                true
                        ) recentCalls.removeFirst()
                        if (recentCalls.size >= 450) {
                            cooldownUntil = recentCalls.first + 3600_000
                            throw IllegalStateException("Hourly API budget")
                        }
                        recentCalls.addLast(now)
                        cache.write(
                            "@rate_budget",
                            recentCalls.map { at -> buildJsonObject { put("at", at) } },
                            now,
                        )
                        // Uma chamada a cada 350 ms, incluindo retries e endpoints diferentes.
                        delay((350 - ((System.nanoTime() / 1_000_000) - lastCall)).coerceAtLeast(0))
                        try {
                            client.get(
                                "https://api.jolpi.ca/ergast/f1/$route.json?limit=100&offset=$offset"
                            )
                        } finally {
                            lastCall = (System.nanoTime() / 1_000_000)
                        }
                    }
                if (response.status.value == 429) {
                    val seconds = response.headers["Retry-After"]?.toLongOrNull() ?: 60
                    network.withLock {
                        cooldownUntil = clock.millis() + seconds.coerceIn(1, 3600) * 1000
                        cache.write(
                            "@cooldown",
                            listOf(buildJsonObject { put("until", cooldownUntil) }),
                            clock.millis(),
                        )
                    }
                    error("API cooldown")
                }
                if (response.status.value in 400..499 && response.status.value != 408)
                    throw NonRetryable("HTTP ${response.status.value}")
                if (response.status.value in 500..599) error("Server unavailable")
                check(response.status.value in 200..299) { "HTTP ${response.status.value}" }
                return json.parseToJsonElement(response.bodyAsText()).jsonObject
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is NonRetryable || attemptIndex == 2 || clock.millis() < cooldownUntil)
                    throw e
                delay(1000L shl attemptIndex)
            }
        }
        error("API unavailable")
    }
}

fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

fun JsonObject.obj(key: String): JsonObject = get(key) as? JsonObject ?: buildJsonObject {}

fun JsonObject.items(key: String): List<JsonObject> =
    (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
