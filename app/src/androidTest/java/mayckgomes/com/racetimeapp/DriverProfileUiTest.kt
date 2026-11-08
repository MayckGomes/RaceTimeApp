package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.presentation.driver.*
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DriverProfileUiTest {
    @get:Rule val compose = createComposeRule()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val clock = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC)
    private val name = "Fixture driver with a very long family name"
    private val profile =
        FavoriteProfile("driver", name, "44", "Fixture nationality", "2000-01-01", "FIX")

    @Test
    fun fullNameAndChampionshipRemainAccessibleWithLargeFont() {
        var openedTeam: String? = null
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 1.5f)
            ) {
                RaceTimeAppTheme {
                    Box(Modifier.width(320.dp)) {
                        DriverDetailsContent(
                            DriverDetailsState("driver", DataBlock(profile)),
                            2026,
                            DataBlock(
                                ChampionshipState(
                                    drivers =
                                        listOf(
                                            Standing(
                                                "driver",
                                                name,
                                                "2",
                                                "120",
                                                "3",
                                                "Fixture Team",
                                                "team",
                                            )
                                        )
                                )
                            ),
                            "",
                            clock,
                            {},
                            { openedTeam = it },
                            {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(name).assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.driver_view_team))
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals("team", openedTeam) }
    }

    @Test
    fun historicalRaceOpensItsOwnSeasonAndRoundAndKeepsRetirementStatus() {
        val race =
            PublishedResult(
                "2024",
                "5",
                "Fixture GP",
                "2024-04-01",
                SessionType.RACE,
                listOf(ResultEntry(driverId = "driver", position = "18", status = "Engine")),
            )
        var opened: PublishedResult? = null
        compose.setContent {
            RaceTimeAppTheme {
                DriverDetailsContent(
                    DriverDetailsState("driver", DataBlock(profile), DataBlock(race)),
                    2026,
                    DataBlock(ChampionshipState()),
                    "",
                    clock,
                    {},
                    {},
                    { opened = it },
                )
            }
        }
        compose
            .onNodeWithText(context.getString(R.string.fav_latest_race))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Engine").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.v2_full_result))
            .performScrollTo()
            .performClick()
        compose.runOnIdle {
            assertEquals("2024", opened?.season)
            assertEquals("5", opened?.round)
        }
    }

    @Test
    fun driverOutsideTheChampionshipRetainsItsNameAndMissingBirthdayDoesNotShowFakeAge() {
        compose.setContent {
            RaceTimeAppTheme {
                DriverDetailsContent(
                    DriverDetailsState(
                        "old",
                        DataBlock(profile.copy(id = "old", dateOfBirth = "")),
                    ),
                    2026,
                    DataBlock(ChampionshipState()),
                    "",
                    clock,
                    {},
                    {},
                    {},
                )
            }
        }
        compose
            .onNodeWithText(context.getString(R.string.fav_not_ranked, 2026))
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.v2_about))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(context.getString(R.string.driver_age, 26)).assertDoesNotExist()
    }
}
