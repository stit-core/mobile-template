package tech.stit.mobile.android.widget

import tech.stit.mobile.core.widget.WidgetState
import tech.stit.mobile.feature.todos.TodayTodosWidgetItem
import tech.stit.mobile.feature.todos.TodayTodosWidgetSnapshot
import kotlin.time.Instant

// Те же задачи, что в mock/fixtures/populated/todos.json — состояния собираются из снимка, не из превью-моков.
internal fun snapshot(state: WidgetState, items: List<TodayTodosWidgetItem> = demoItems) = TodayTodosWidgetSnapshot(
    state = state,
    items = if (state == WidgetState.Populated || state == WidgetState.Stale) items else emptyList(),
    generatedAt = Instant.parse("2026-09-17T12:00:00Z"),
    tapUrl = "https://app.demo.invalid/todos",
)

internal val demoItems = listOf(
    TodayTodosWidgetItem("todo-3", "Проверить deep link todos/todo-3", Instant.parse("2026-09-12T11:00:00Z"), done = false),
    TodayTodosWidgetItem("todo-1", "Прочитать docs/reference/architecture.md", Instant.parse("2026-09-10T08:00:00Z"), done = false),
    TodayTodosWidgetItem("todo-2", "Собрать demo-сборку", Instant.parse("2026-09-12T10:15:00Z"), done = true),
)
