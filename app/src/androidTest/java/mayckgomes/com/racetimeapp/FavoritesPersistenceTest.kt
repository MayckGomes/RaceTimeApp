package mayckgomes.com.racetimeapp

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.data.local.FavoritesStore
import org.junit.Assert.*
import org.junit.Test

class FavoritesPersistenceTest {
    @Test
    fun legacyEntriesSurviveRecreationRemovalAndUndoWithoutMigration() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "test_favorites_${System.nanoTime()}_"
        // Testes usam arquivos isolados; nunca limpam os favoritos reais do usuário.
        val context =
            object : ContextWrapper(base) {
                override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                    base.getSharedPreferences(prefix + name, mode)
            }
        val drivers = context.getSharedPreferences("driversFavorites", Context.MODE_PRIVATE)
        val teams = context.getSharedPreferences("teamsFavorites", Context.MODE_PRIVATE)
        try {
            assertTrue(
                drivers
                    .edit()
                    .putString("old_driver", "Legacy Driver Label")
                    .putString("other", "Other Label")
                    .commit()
            )
            assertTrue(teams.edit().putString("old_team", "Legacy Team Label").commit())
            val store = FavoritesStore(context)
            assertEquals("Legacy Driver Label", store.ids.value.drivers["old_driver"])
            assertEquals("Legacy Team Label", store.ids.value.constructors["old_team"])
            assertTrue(store.remove("old_driver", false))
            val recreated = FavoritesStore(context)
            assertFalse("old_driver" in recreated.ids.value.drivers)
            assertEquals("Other Label", recreated.ids.value.drivers["other"])
            assertEquals("Legacy Team Label", recreated.ids.value.constructors["old_team"])
            assertTrue(recreated.add("old_driver", "Legacy Driver Label", false))
            assertEquals(
                "Legacy Driver Label",
                FavoritesStore(context).ids.value.drivers["old_driver"],
            )
        } finally {
            drivers.edit().clear().commit()
            teams.edit().clear().commit()
            base.deleteSharedPreferences(prefix + "driversFavorites")
            base.deleteSharedPreferences(prefix + "teamsFavorites")
        }
    }
}
