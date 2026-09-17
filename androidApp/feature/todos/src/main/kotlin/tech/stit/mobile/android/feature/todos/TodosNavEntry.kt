package tech.stit.mobile.android.feature.todos

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import tech.stit.mobile.android.core.navigation.Destination

fun EntryProviderScope<NavKey>.todosEntries(onOpenTodo: (String) -> Unit, onBack: () -> Unit) {
    entry<Destination.TodoList> { TodoListRoute(onOpenTodo = onOpenTodo) }
    entry<Destination.TodoDetail> { key -> TodoDetailRoute(id = key.id, onBack = onBack) }
}
