package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.domain.models.RacesCalendar
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardCardsTest {
    @get:Rule val compose = createComposeRule()
    private val androidContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun scheduledWindowShowsTheCautionInsteadOfClaimingLiveActivity() {
        val race = RacesCalendar("1", "Fixture Grand Prix", "2026-04-09", "14:00:00Z", "2026")
        val session = race.sessions().single()
        val state =
            WeekendContext(WeekendState.SESSION_IN_PROGRESS, race, session, null, null, null, null)
        compose.setContent {
            RaceTimeAppTheme {
                Box(Modifier.width(320.dp)) {
                    WeekendHeroCard(state, Instant.parse("2026-04-09T14:10:00Z"), false) {}
                }
            }
        }
        compose.onNodeWithText("Fixture Grand Prix").assertIsDisplayed()
        compose
            .onNodeWithText(androidContext.getString(R.string.v2_scheduled_caution))
            .assertIsDisplayed()
    }

    @Test
    fun cachedFailureKeepsRetryActionFunctional() {
        var retries = 0
        compose.setContent {
            RaceTimeAppTheme {
                androidx.compose.foundation.layout.Column {
                    DataNotice(true, true, 1) { retries++ }
                }
            }
        }
        compose.onNodeWithText(androidContext.getString(R.string.home_retry)).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun fullResultActionOpensTheSelectedRealSessionType() {
        var opened = 0
        val result =
            PublishedResult(
                "2026",
                "1",
                "Fixture Grand Prix",
                "2026-04-09",
                SessionType.RACE,
                listOf(
                    ResultEntry("1", driverId = "fixture", name = "Fixture Driver", points = "25")
                ),
            )
        compose.setContent {
            RaceTimeAppTheme {
                DashboardResultCard(
                    ResultSnapshot(result, false, false, 0),
                    "2026-04-09",
                    listOf(SessionType.RACE),
                    onSelect = {},
                    onRetry = {},
                    onOpen = { opened++ },
                )
            }
        }
        compose.onNodeWithText(androidContext.getString(R.string.v2_full_result)).performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
