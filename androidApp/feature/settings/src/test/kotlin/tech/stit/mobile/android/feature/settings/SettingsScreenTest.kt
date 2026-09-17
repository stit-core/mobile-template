package tech.stit.mobile.android.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.feature.demo.DemoPanelIntent
import tech.stit.mobile.feature.demo.DemoPanelState
import tech.stit.mobile.feature.settings.AppLanguage
import tech.stit.mobile.feature.settings.SettingsIntent
import tech.stit.mobile.feature.settings.ThemeMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<SettingsIntent>()
    private val demoIntents = mutableListOf<DemoPanelIntent>()
    private val links = mutableListOf<String>()
    private var notifications = 0

    private fun show(demo: DemoPanelState = DemoPanelState()) {
        composeRule.setContent {
            StitTheme {
                SettingsScreen(
                    state = demoSettings,
                    demo = demo,
                    onIntent = intents::add,
                    onDemoIntent = demoIntents::add,
                    onOpenNotifications = { notifications++ },
                    onOpenLink = links::add,
                )
            }
        }
    }

    @Test
    fun `тема и язык уходят интентами`() {
        show()
        composeRule.onNodeWithTag(SettingsTestTags.theme("Dark")).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.language("Ru")).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(SettingsIntent.SetTheme(ThemeMode.Dark), SettingsIntent.SetLanguage(AppLanguage.Ru)), intents)
    }

    @Test
    fun `уведомления и правовые ссылки открываются, demo-панели нет против настоящего бэкенда`() {
        show()
        val list = composeRule.onNodeWithTag(SettingsTestTags.LIST)
        list.performScrollToNode(hasText("Version 1.0.0 (1000000)"))
        composeRule.onNodeWithText("Version 1.0.0 (1000000)").assertIsDisplayed()
        list.performScrollToNode(hasText("Notifications"))
        composeRule.onNodeWithText("Notifications").performClick()
        list.performScrollToNode(hasText("Privacy policy"))
        composeRule.onNodeWithText("Privacy policy").performClick()
        composeRule.onNodeWithText("Demo mode").assertDoesNotExist()
        composeRule.waitForIdle()
        assertEquals(1, notifications)
        assertEquals(listOf("https://app.example.com/privacy"), links)
    }

    @Test
    fun `demo-панель переключает сценарий и задержку`() {
        show(DemoPanelState(available = true, latencyMs = 0))
        val list = composeRule.onNodeWithTag(SettingsTestTags.LIST)
        list.performScrollToNode(hasTestTag(MockScenario.Empty.testTag()))
        composeRule.onNodeWithTag(MockScenario.Empty.testTag()).performClick()
        list.performScrollToNode(hasTestTag(SettingsTestTags.DEMO_LATENCY))
        composeRule.onNodeWithTag(SettingsTestTags.DEMO_LATENCY).performTextReplacement("250")
        composeRule.waitForIdle()
        assertEquals(listOf(DemoPanelIntent.SelectScenario(MockScenario.Empty), DemoPanelIntent.SetLatency(250)), demoIntents)
    }
}
