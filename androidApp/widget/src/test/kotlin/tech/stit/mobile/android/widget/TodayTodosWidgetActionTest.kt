package tech.stit.mobile.android.widget

import android.app.Application
import androidx.glance.GlanceId
import androidx.glance.action.actionParametersOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.core.testing.demoConfig
import tech.stit.mobile.core.widget.WidgetState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Кнопка строки виджета: система зовёт колбэк вне процесса приложения, а он — общий обработчик действия.
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class TodayTodosWidgetActionTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var container: SharedContainer

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun inject() = hiltRule.inject()

    @Test
    fun `отметка закрывает задачу и обновляет снимок виджета`() = runBlocking {
        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()

        TodayTodosWidgetAction().onAction(application, TestGlanceId, parameters("todo-1"))

        assertTrue(container.todoRepository.todos.value.single { it.id == "todo-1" }.done)
        val snapshot = container.todayTodosWidgetProvider().snapshot()
        assertEquals(WidgetState.Populated, snapshot.state)
        assertTrue(snapshot.items.single { it.id == "todo-1" }.done)
    }

    @Test
    fun `без сессии задача не трогается и снимок остаётся пустым`() = runBlocking {
        TodayTodosWidgetAction().onAction(application, TestGlanceId, parameters("todo-1"))

        assertTrue(container.todoRepository.todos.value.isEmpty())
        assertEquals(WidgetState.SignedOut, container.todayTodosWidgetProvider().snapshot().state)
    }

    @Test
    fun `колбэк без адресата в действие не идёт`() = runBlocking {
        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()

        TodayTodosWidgetAction().onAction(application, TestGlanceId, actionParametersOf())

        assertFalse(container.todoRepository.todos.value.any { it.done && it.id == "todo-1" })
    }

    private fun parameters(todoId: String) = actionParametersOf(TodayTodosWidgetAction.TODO_ID to todoId)

    private object TestGlanceId : GlanceId
}

// Граф виджета даёт SharedContainer приложение; в тесте модуля его собирают на фейках ОС и MockTransport.
@Module
@InstallIn(SingletonComponent::class)
internal object TestWidgetContainerModule {
    @Provides
    @Singleton
    fun sharedContainer(application: Application): SharedContainer =
        SharedContainer(FakePlatformDeps(application.filesDir.absolutePath), demoConfig())
}
