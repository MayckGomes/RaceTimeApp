package mayckgomes.com.racetimeapp

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.*
import mayckgomes.com.racetimeapp.data.local.PreferenceResponseCache
import org.junit.Assert.*
import org.junit.Test

class PersistentCacheTest {
    @Test
    fun cacheRowsAndTheirTimestampSurviveRecreatingTheAdapter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_cache_${System.nanoTime()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            val rows =
                listOf(Json.parseToJsonElement("""{"season":"2026","round":"1"}""").jsonObject)
            PreferenceResponseCache(prefs).write("2026/races", rows, 123456L)
            // commit aguarda as gravações apply pendentes antes de recriar o leitor.
            assertTrue(prefs.edit().commit())
            val recreated =
                PreferenceResponseCache(context.getSharedPreferences(name, Context.MODE_PRIVATE))
            assertEquals(rows, recreated.read("2026/races")?.rows)
            assertEquals(123456L, recreated.read("2026/races")?.savedAt)
            assertNull(recreated.read("2025/races"))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }

    @Test
    fun corruptedCacheIsIgnoredWithoutBreakingOtherSavedRoutes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_cache_${System.nanoTime()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            assertTrue(prefs.edit().putString("jolpica:2026/races", "not-json").commit())
            val cache = PreferenceResponseCache(prefs)
            assertNull(cache.read("2026/races"))
            cache.write("2026/driverstandings", emptyList(), 123456L)
            assertNotNull(cache.read("2026/driverstandings"))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
