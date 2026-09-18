package tech.stit.mobile.android.widget

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.testing.unit.hasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.core.widget.WidgetState
import java.util.TimeZone
import kotlin.test.assertTrue

// Пять состояний снимка рисуются без сети и репозиториев: виджету дают готовый срез.
@RunWith(AndroidJUnit4::class)
class TodayTodosWidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Срок в строке задачи не зависит от пояса машины, на которой шёл тест.
    @Before
    fun pinTimeZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    @Test
    fun `placeholder — виджет говорит, что данные ещё едут`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.Placeholder)) }

        onNode(hasText("Nearest tasks")).assertExists()
        onNode(hasText("Loading tasks…")).assertExists()
    }

    @Test
    fun `populated — задачи, сроки и отметки`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.Populated)) }

        onNode(hasText("Проверить deep link todos/todo-3")).assertExists()
        onNode(hasText("Прочитать docs/reference/architecture.md")).assertExists()
        onNode(hasText("Собрать demo-сборку")).assertExists()
        onNode(hasText("Updated 9/10/26")).assertExists()
        onAllNodes(hasText("Updated 9/12/26")).assertCountEquals(2)
        onAllNodes(hasText("Mark done")).assertCountEquals(2)
        onNode(hasText("Done")).assertExists()
    }

    @Test
    fun `кнопка строки зовёт колбэк с id незакрытой задачи, у закрытой кнопки нет`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.Populated)) }

        onNode(
            hasRunCallbackClickAction<TodayTodosWidgetAction>(actionParametersOf(TodayTodosWidgetAction.TODO_ID to "todo-1")),
        ).assertExists()
        onNode(
            hasRunCallbackClickAction<TodayTodosWidgetAction>(actionParametersOf(TodayTodosWidgetAction.TODO_ID to "todo-2")),
        ).assertDoesNotExist()
    }

    @Test
    fun `empty — задач нет`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.Empty)) }

        onNode(hasText("No tasks")).assertExists()
        onNode(hasText("Собрать demo-сборку")).assertDoesNotExist()
    }

    @Test
    fun `signed-out — приглашение войти вместо чужих задач`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.SignedOut)) }

        onNode(hasText("Sign in to see tasks")).assertExists()
        onNode(hasText("Проверить deep link todos/todo-3")).assertDoesNotExist()
    }

    @Test
    fun `stale — предупреждение над теми же задачами`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.Stale)) }

        onNode(hasText("Data may be outdated")).assertExists()
        onNode(hasText("Проверить deep link todos/todo-3")).assertExists()
    }

    @Test
    fun `тап открывает приложение по той же ссылке, что deep link`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayTodosWidgetBody(snapshot(WidgetState.Populated)) }

        val expected = Intent(Intent.ACTION_VIEW, "https://app.demo.invalid/todos".toUri()).setPackage(context.packageName)
        onNode(hasStartActivityClickAction(expected)).assertExists()
    }

    @Test
    fun `приёмник отдаёт системе тот же виджет`() {
        assertTrue(TodayTodosWidgetReceiver().glanceAppWidget is TodayTodosWidget)
    }
}
