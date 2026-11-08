package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.components.championship.*
import mayckgomes.com.racetimeapp.domain.weekend.Standing
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StandingItemTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longNamesRemainVisibleWithLargeFontAndOpenDetails() {
        val name = "Fixture driver with a long display name"
        var opened = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.5f)) {
                RaceTimeAppTheme {
                    Box(Modifier.width(320.dp)) {
                        StandingItem(
                            Standing("id", name, "1", "100", "2", "Fixture Constructor"),
                            delta = null,
                            favorite = false,
                            constructor = false,
                            onOpen = { opened++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(name, substring = true).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithText("=").assertDoesNotExist()
    }

    @Test
    fun movementHasAnAccessibleDescription() {
        compose.setContent { RaceTimeAppTheme { MovementIndicator(2) } }
        val description =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getString(R.string.champ_moved_up, 2)
        compose.onNodeWithContentDescription(description).assertIsDisplayed()
    }
}
