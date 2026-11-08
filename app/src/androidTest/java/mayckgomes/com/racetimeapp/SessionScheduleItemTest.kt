package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.components.calendar.SessionScheduleItem
import mayckgomes.com.racetimeapp.components.home.localDate
import mayckgomes.com.racetimeapp.domain.weekend.*
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionScheduleItemTest {
    @get:Rule val compose = createComposeRule()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun unknownTimeIsExplicitAndDoesNotInventAResultAction() {
        compose.setContent {
            RaceTimeAppTheme {
                Box(Modifier.width(320.dp)) {
                    SessionScheduleItem(
                        Session("2026", "1", SessionType.FP1, "2026-04-09", null),
                        SessionStatus.TIME_UNKNOWN,
                    )
                }
            }
        }
        compose
            .onNodeWithText(context.getString(R.string.gp_date_utc, localDate("2026-04-09")))
            .assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.v2_no_results_endpoint))
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.v2_full_result)).assertDoesNotExist()
    }

    @Test
    fun publishedResultProvidesAWorkingNavigationAction() {
        var opened = 0
        compose.setContent {
            RaceTimeAppTheme {
                SessionScheduleItem(
                    Session("2026", "1", SessionType.RACE, "2026-04-09", "14:00:00Z"),
                    SessionStatus.RESULT_PUBLISHED,
                    onResult = { opened++ },
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.v2_full_result)).performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
