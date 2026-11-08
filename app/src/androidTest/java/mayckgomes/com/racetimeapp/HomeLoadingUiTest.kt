package mayckgomes.com.racetimeapp

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import mayckgomes.com.racetimeapp.components.home.*
import mayckgomes.com.racetimeapp.ui.theme.RaceTimeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeLoadingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun skeletonHasALoadingDescriptionAndNoInteractivePlaceholder() {
        compose.setContent { RaceTimeAppTheme { HomeCardSkeleton(HomeSkeletonKind.WEEKEND) } }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose
            .onNodeWithContentDescription(context.getString(R.string.home_loading_skeleton))
            .assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun styledActionsAndFavoriteCardsPreserveTheirCallbacks() {
        var opened = 0
        var favorite = 0
        compose.setContent {
            RaceTimeAppTheme {
                Column {
                    RaceActionButton("Fixture action", { opened++ })
                    HomeFavoriteRow("Fixture favorite", listOf("Fixture details")) { favorite++ }
                }
            }
        }
        compose.onNodeWithText("Fixture action").performClick()
        compose.onNodeWithText("Fixture favorite").performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, favorite)
        }
    }
}
