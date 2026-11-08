package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.components.favorites.FavoriteCard
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoriteCardTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unrankedFavoriteRetainsLongNameAndAllowsOpeningAndRemoval() {
        var opened = 0
        var removed = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "Fixture saved driver with a very long display name"
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 1.5f)
            ) {
                RaceTimeAppTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        FavoriteCard(
                            FavoriteSummary("old", name, false, null, null, null),
                            2026,
                            false,
                            false,
                            onOpen = { opened++ },
                            onRemove = { removed++ },
                            onRetry = {},
                            onResult = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(name).assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.fav_not_ranked, 2026))
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.fav_details))
            .performScrollTo()
            .performClick()
        compose
            .onNodeWithText(context.getString(R.string.fav_remove))
            .performScrollTo()
            .performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, removed)
        }
    }

    @Test
    fun raceActionKeepsTheCorrespondingSeasonAndRound() {
        val result =
            PublishedResult(
                "2024",
                "3",
                "Fixture GP",
                "2024-04-01",
                SessionType.RACE,
                listOf(ResultEntry(position = "4", driverId = "old", status = "Engine")),
            )
        var opened: PublishedResult? = null
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            RaceTimeAppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FavoriteCard(
                        FavoriteSummary(
                            "old",
                            "Saved",
                            false,
                            null,
                            null,
                            FavoriteDetails(recentRace = DataBlock(result)),
                        ),
                        2026,
                        false,
                        false,
                        onOpen = {},
                        onRemove = {},
                        onRetry = {},
                        onResult = { opened = it },
                    )
                }
            }
        }
        compose.onNodeWithText("Engine").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.v2_full_result))
            .performScrollTo()
            .performClick()
        compose.runOnIdle {
            assertEquals("2024", opened?.season)
            assertEquals("3", opened?.round)
        }
    }
}
