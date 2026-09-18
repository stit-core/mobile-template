package tech.stit.mobile.android.core.platform.action

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.core.testing.demoConfig
import tech.stit.mobile.core.mock.MockScenario
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Голос и автоматизации получают один короткий ответ на каждый исход: тексты — ресурсы платформы, решение — shared.
@RunWith(AndroidJUnit4::class)
class AddTodoActionHandlerTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val container = SharedContainer(FakePlatformDeps(application.filesDir.absolutePath), demoConfig())
    private val handler = AddTodoActionHandler(application, container)

    @Test
    fun `задача заводится и ответ называет её заголовок`() = runBlocking {
        container.sessionRepository.login("demo@example.com", "password")

        assertEquals("Added: Купить молоко", handler.add("Купить молоко"))
        assertTrue(container.todoRepository.todos.value.any { it.title == "Купить молоко" })
    }

    @Test
    fun `без сессии ассистент просит войти, а не логинится сам`() = runBlocking {
        assertEquals("Sign in to the app to add tasks.", handler.add("Купить молоко"))
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }

    @Test
    fun `отказ API и пустой текст отвечают одинаково понятно`() = runBlocking {
        container.sessionRepository.login("demo@example.com", "password")

        assertEquals("Could not add the task. Try again later.", handler.add("   "))
        checkNotNull(container.mockTransport).reset(MockScenario.Error)
        assertEquals("Could not add the task. Try again later.", handler.add("Купить молоко"))
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }
}
