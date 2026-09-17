package tech.stit.mobile.android.feature.todos

import tech.stit.mobile.entity.todo.Todo
import kotlin.time.Instant

// Те же задачи, что в mock/fixtures/populated/todos.json.
internal val demoTodos = listOf(
    Todo(
        "todo-1",
        "Прочитать docs/reference/architecture.md",
        false,
        Instant.parse("2026-09-10T08:00:00Z"),
        Instant.parse("2026-09-10T08:00:00Z"),
        1,
    ),
    Todo("todo-2", "Собрать demo-сборку", true, Instant.parse("2026-09-11T09:30:00Z"), Instant.parse("2026-09-12T10:15:00Z"), 2),
    Todo(
        "todo-3",
        "Проверить deep link todos/todo-3",
        false,
        Instant.parse("2026-09-12T11:00:00Z"),
        Instant.parse("2026-09-12T11:00:00Z"),
        1,
    ),
)
