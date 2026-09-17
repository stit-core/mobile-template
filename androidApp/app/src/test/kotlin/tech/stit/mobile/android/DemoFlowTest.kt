package tech.stit.mobile.android

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.feature.auth.AuthTestTags
import tech.stit.mobile.android.feature.todos.TodosTestTags
import tech.stit.mobile.core.lib.SettingsKeys
import java.io.File
import javax.inject.Inject

// Demo-сборка от старта до задачи: те же фикстуры mock/, что читает FixturesTest в shared — второго набора данных нет.
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class DemoFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var platform: FakePlatformDeps

    private val fixtureTitles: List<String> by lazy {
        val json = Json.parseToJsonElement(File("../../mock/fixtures/populated/todos.json").readText())
        json.jsonObject.getValue("data").jsonArray.map { it.jsonObject.getValue("title").jsonPrimitive.content }
    }

    @Test
    fun `вход в demo показывает задачи из фикстуры populated`() {
        hiltRule.inject()
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodesWithTag(AuthTestTags.LOGIN_EMAIL).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_EMAIL).performTextInput("demo@example.com")
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_PASSWORD).performTextInput("password")
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_SUBMIT).performClick()

            composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodesWithTag(TodosTestTags.LIST).fetchSemanticsNodes().isNotEmpty() }
            fixtureTitles.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

            composeRule.onNodeWithTag(TodosTestTags.item("todo-2")).performClick()
            composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithText("Собрать demo-сборку").assertIsDisplayed()
        }
    }

    @Test
    fun `deep link ждёт входа и открывает задачу после него`() {
        hiltRule.inject()
        val intent = Intent(Intent.ACTION_VIEW, "https://app.demo.invalid/todos/todo-3".toUri())
            .setClass(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodesWithTag(AuthTestTags.LOGIN_EMAIL).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_EMAIL).performTextInput("demo@example.com")
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_PASSWORD).performTextInput("password")
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_SUBMIT).performClick()

            composeRule.waitUntil(TIMEOUT_MS) {
                composeRule.onAllNodesWithText("Проверить deep link todos/todo-3").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("In progress").assertIsDisplayed()
        }
    }

    @Test
    fun `сценарий authenticated стартует без экрана входа`() {
        hiltRule.inject()
        platform.settings.values[SettingsKeys.MOCK_SCENARIO] = "authenticated"
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(TIMEOUT_MS) { composeRule.onAllNodes(hasTestTag(TodosTestTags.LIST)).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(AuthTestTags.LOGIN_EMAIL).assertDoesNotExist()
            composeRule.onNodeWithText(fixtureTitles.first()).assertIsDisplayed()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
