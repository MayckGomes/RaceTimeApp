package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import mayckgomes.com.racetimeapp.presentation.settings.NotificationPreferenceItem
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationPreferenceItemTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun wholeRowCanToggleWithALongLabelAndLargeFont() {
        val label = "Fixture sprint qualifying preference with a long label"
        compose.setContent {
            var checked by remember { mutableStateOf(false) }
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, 1.5f)
            ) {
                RaceTimeAppTheme {
                    Box(Modifier.width(300.dp)) {
                        NotificationPreferenceItem(label, checked, true) { checked = it }
                    }
                }
            }
        }
        compose.onNodeWithText(label).assertIsDisplayed().assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun disabledPreferenceCannotChangeTheChoice() {
        var calls = 0
        compose.setContent {
            RaceTimeAppTheme { NotificationPreferenceItem("Fixture FP1", false, false) { calls++ } }
        }
        compose.onNodeWithText("Fixture FP1").assertIsNotEnabled().assertIsOff()
        compose.runOnIdle { assertEquals(0, calls) }
    }
}
