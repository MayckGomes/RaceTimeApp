package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.components.statistics.*
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.presentation.gp.*
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RaceStatisticsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val one =
        ResultEntry(
            driverId = "one",
            name = "Fixture driver with a long display name",
            position = "1",
            grid = "3",
            status = "Finished",
            fastestRank = "1",
            fastestTime = "1:23.456",
        )
    private val two =
        ResultEntry(
            driverId = "two",
            name = "Fixture second driver",
            position = "2",
            grid = "4",
            status = "Finished",
        )
    private val result =
        PublishedResult("2026", "1", "Fixture GP", "2026-04-01", SessionType.RACE, listOf(one, two))
    private val state
        get() =
            GrandPrixState(
                loading = false,
                results = mapOf(SessionType.RACE to DataBlock(listOf(result))),
            )

    @Test
    fun highlightsRemainAccessibleWithLongNamesAndLargeFont() {
        var opened: String? = null
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 1.5f)
            ) {
                RaceTimeAppTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        RaceHighlightsCard(raceStatistics(result)!!) { opened = it }
                    }
                }
            }
        }
        compose.onNodeWithText(one.name).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("one", opened) }
        compose.onNodeWithText("1:23.456").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.stat_gain_driver, one.name, 2))
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.stat_gain_driver, two.name, 2))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun retirementStatusDoesNotShowAComputedGain() {
        compose.setContent {
            RaceTimeAppTheme {
                DriverStatisticsRow(
                    one.copy(position = "10", grid = "12", status = "Engine", fastestTime = ""),
                    null,
                ) {}
            }
        }
        compose.onNodeWithText("Engine").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.stat_retired)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.stat_gained, 2)).assertDoesNotExist()
    }

    @Test
    fun pitStopsAreRequestedOnlyAfterTheUserPressesLoad() {
        var calls = 0
        compose.setContent {
            RaceTimeAppTheme {
                LazyColumn {
                    grandPrixStatistics(
                        state,
                        2026,
                        "1",
                        null,
                        1,
                        null,
                        {},
                        {},
                        {},
                        { calls++ },
                        { _, _ -> },
                        {},
                        {},
                    )
                }
            }
        }
        compose.runOnIdle { assertEquals(0, calls) }
        compose
            .onNodeWithText(context.getString(R.string.stat_load_pits))
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun selectingADriverDoesNotAutomaticallyFetchTheirLaps() {
        var calls = 0
        var loaded: String? = null
        compose.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            RaceTimeAppTheme {
                LazyColumn {
                    grandPrixStatistics(
                        state,
                        2026,
                        "1",
                        null,
                        2,
                        selected,
                        {},
                        {},
                        { selected = it },
                        {},
                        { id, _ ->
                            calls++
                            loaded = id
                        },
                        {},
                        {},
                    )
                }
            }
        }
        compose
            .onNodeWithText(context.getString(R.string.stat_choose_driver, one.name))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(two.name).performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose
            .onNodeWithText(context.getString(R.string.stat_load_laps))
            .performScrollTo()
            .performClick()
        compose.runOnIdle {
            assertEquals(1, calls)
            assertEquals("two", loaded)
        }
    }

    @Test
    fun failedAndSuccessfullyEmptyExtraResponsesHaveDifferentMessages() {
        val failed = mutableStateOf(false)
        compose.setContent {
            RaceTimeAppTheme {
                LazyColumn {
                    grandPrixStatistics(
                        state.copy(pitStops = DataBlock(emptyList(), failed = failed.value)),
                        2026,
                        "1",
                        null,
                        1,
                        null,
                        {},
                        {},
                        {},
                        {},
                        { _, _ -> },
                        {},
                        {},
                    )
                }
            }
        }
        compose
            .onNodeWithText(context.getString(R.string.stat_no_extra))
            .performScrollTo()
            .assertIsDisplayed()
        compose.runOnIdle { failed.value = true }
        compose.onNodeWithText(context.getString(R.string.stat_no_extra)).assertDoesNotExist()
        compose
            .onNodeWithText(context.getString(R.string.home_load_error))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
