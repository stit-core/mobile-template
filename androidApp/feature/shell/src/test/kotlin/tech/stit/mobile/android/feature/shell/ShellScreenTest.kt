package tech.stit.mobile.android.feature.shell

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.ui.UiTestTags
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.requirements.AppRequirements
import tech.stit.mobile.core.requirements.RequirementsStatus
import tech.stit.mobile.feature.requirements.RequirementsState
import tech.stit.mobile.feature.shell.ShellState
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ShellScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `пока сессия неизвестна — загрузка, контент скрыт`() {
        composeRule.setContent { StitTheme { Shell(shell = ShellState(auth = AuthState.Unknown)) } }
        composeRule.onNodeWithTag(UiTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithText("content").assertDoesNotExist()
    }

    @Test
    fun `без сессии вкладок нет`() {
        composeRule.setContent { StitTheme { Shell(shell = ShellState(auth = AuthState.SignedOut)) } }
        composeRule.onNodeWithText("content").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun `офлайн — баннер над контентом`() {
        composeRule.setContent { StitTheme { Shell(shell = ShellState(auth = AuthState.SignedIn, online = false)) } }
        composeRule.onNodeWithTag(UiTestTags.OFFLINE_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("content").assertIsDisplayed()
    }

    @Test
    fun `update_required закрывает контент, maintenance даёт повтор`() {
        var retries = 0
        composeRule.setContent {
            StitTheme {
                Shell(
                    shell = ShellState(auth = AuthState.SignedIn),
                    requirements = requirements(RequirementsStatus.Maintenance),
                    onRetryRequirements = { retries++ },
                )
            }
        }
        composeRule.onNodeWithText("content").assertDoesNotExist()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retries)
    }

    @Test
    fun `замок скрывает контент и зовёт onUnlock`() {
        var unlocks = 0
        composeRule.setContent {
            StitTheme { Shell(shell = ShellState(auth = AuthState.SignedIn), locked = true, onUnlock = { unlocks++ }) }
        }
        composeRule.onNodeWithText("content").assertDoesNotExist()
        composeRule.onNodeWithText("Unlock").performClick()
        composeRule.waitForIdle()
        assertEquals(1, unlocks)
    }

    @Test
    fun `рекомендация обновиться скрывается, вкладка переключается`() {
        var dismissed = 0
        var selected: ShellTab? = null
        composeRule.setContent {
            StitTheme {
                Shell(
                    shell = ShellState(auth = AuthState.SignedIn),
                    requirements = requirements(RequirementsStatus.UpdateRecommended),
                    onDismissRecommendation = { dismissed++ },
                    onTabSelect = { selected = it },
                )
            }
        }
        composeRule.onNodeWithText("Dismiss").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.waitForIdle()
        assertEquals(1, dismissed)
        assertEquals(ShellTab.Settings, selected)
    }

    private fun requirements(status: RequirementsStatus) =
        RequirementsState(checked = true, requirements = AppRequirements.Default.copy(status = status, latestVersion = "1.2.0"))
}

@Composable
internal fun Shell(
    shell: ShellState,
    requirements: RequirementsState = RequirementsState(),
    locked: Boolean = false,
    onTabSelect: (ShellTab) -> Unit = {},
    onUnlock: () -> Unit = {},
    onRetryRequirements: () -> Unit = {},
    onDismissRecommendation: () -> Unit = {},
) {
    ShellScreen(
        shell = shell,
        requirements = requirements,
        locked = locked,
        currentTab = ShellTab.Todos,
        onTabSelect = onTabSelect,
        onUnlock = onUnlock,
        onOpenStore = {},
        onRetryRequirements = onRetryRequirements,
        onDismissRecommendation = onDismissRecommendation,
    ) { Text(text = "content") }
}
