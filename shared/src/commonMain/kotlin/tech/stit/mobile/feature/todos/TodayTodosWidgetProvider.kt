package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.lib.ConnectivityMonitor
import tech.stit.mobile.core.lib.FileCache
import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.entity.todo.Todo
import tech.stit.mobile.entity.todo.TodoRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

// Снимок для виджета: publish() кладёт срез в общий каталог, snapshot() отдаёт его поверхности вместе с состоянием.
class TodayTodosWidgetProvider internal constructor(
    private val todos: TodoRepository,
    private val session: SessionStore,
    private val connectivity: ConnectivityMonitor,
    private val cache: FileCache<TodayTodosWidgetSnapshot>,
    private val deepLinkHost: String,
    private val clock: Clock,
) {
    suspend fun snapshot(): TodayTodosWidgetSnapshot {
        if (session.authState.value == AuthState.SignedOut) return snapshotOf(WidgetState.SignedOut)
        val published = cache.read()
        return when {
            published == null -> snapshotOf(WidgetState.Placeholder)

            // Сеть есть — снимок выровняется на следующем обновлении, и предупреждать пользователя не о чем.
            published.isOutdated() && !connectivity.online.value -> published.copy(state = WidgetState.Stale)

            else -> published
        }
    }

    suspend fun publish(): TodayTodosWidgetSnapshot = build().also { cache.write(it) }

    private suspend fun build(): TodayTodosWidgetSnapshot {
        if (session.authState.value == AuthState.SignedOut) return snapshotOf(WidgetState.SignedOut)
        if (todos.todos.value.isEmpty()) todos.restore()
        val items = todos.todos.value.sortedWith(NEAREST_FIRST).take(LIMIT).map {
            TodayTodosWidgetItem(id = it.id, title = it.title, updatedAt = it.updatedAt, done = it.done)
        }
        return snapshotOf(if (items.isEmpty()) WidgetState.Empty else WidgetState.Populated, items)
    }

    private fun TodayTodosWidgetSnapshot.isOutdated(): Boolean = clock.now() - generatedAt > FRESH_FOR

    private fun snapshotOf(state: WidgetState, items: List<TodayTodosWidgetItem> = emptyList()): TodayTodosWidgetSnapshot =
        TodayTodosWidgetSnapshot(
            state = state,
            items = items,
            generatedAt = clock.now(),
            tapUrl = "https://$deepLinkHost/todos",
        )

    companion object {
        const val SLUG = "today-todos"

        private const val LIMIT = 3

        // Требование свежести — «в пределах часа»; интервалы обновления задаёт система, не этот порог.
        private val FRESH_FOR = 1.hours

        // Срока у задачи в контракте нет, поэтому ближайшие — незакрытые с самым свежим изменением.
        private val NEAREST_FIRST = compareBy<Todo> { it.done }.thenByDescending { it.updatedAt }
    }
}
