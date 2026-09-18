package tech.stit.mobile.android.widget

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.core.testing.demoConfig
import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.feature.todos.TodayTodosWidgetProvider
import kotlin.test.assertEquals

// Обновление виджета: снимок публикует shared, перерисовку заказывает Glance, а зовут обе половины порт и коллектор.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WidgetRefreshTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `порт публикует снимок задач и не падает без установленных виджетов`() = runBlocking {
        val container = container()
        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()

        GlanceWidgetRefresher(application, container).refresh(TodayTodosWidgetProvider.SLUG)

        assertEquals(WidgetState.Populated, container.todayTodosWidgetProvider().snapshot().state)
    }

    @Test
    fun `чужой slug снимок не трогает`() = runBlocking {
        val container = container()
        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()

        GlanceWidgetRefresher(application, container).refresh("other-widget")

        assertEquals(WidgetState.Placeholder, container.todayTodosWidgetProvider().snapshot().state)
    }

    @Test
    fun `изменение задач в приложении переносится в снимок виджета`() = runTest {
        val container = container()
        val refresher = RecordingRefresher()
        // Коллектор — на нетерпеливом диспетчере теста: публикация видна сразу после подписки и после каждого изменения.
        val collector = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

        WidgetUpdates(container, refresher).start(collector)
        assertEquals(listOf(TodayTodosWidgetProvider.SLUG), refresher.slugs)

        container.sessionRepository.login("demo@example.com", "password")
        container.todoRepository.refresh()

        assertEquals(2, refresher.slugs.size)
    }

    // filesDir у Robolectric свой на каждый тест: снимок и кеш задач не переезжают между прогонами.
    private fun container() = SharedContainer(FakePlatformDeps(application.filesDir.absolutePath), demoConfig())

    private class RecordingRefresher : WidgetRefresher {
        val slugs = mutableListOf<String>()

        override suspend fun refresh(slug: String) {
            slugs += slug
        }
    }
}
