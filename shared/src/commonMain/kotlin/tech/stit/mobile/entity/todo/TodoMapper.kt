package tech.stit.mobile.entity.todo

import tech.stit.mobile.core.api.generated.TodoView

internal fun TodoView.toModel(): Todo = Todo(
    id = id,
    title = title,
    done = done,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
)
