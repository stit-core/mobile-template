package tech.stit.mobile.feature.todos

import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.todo.Todo

data class TodoDetailState(
    val todo: Todo? = null,
    val loading: Boolean = false,
    val error: MessageKey? = null,
    val offline: Boolean = false,
    val deleted: Boolean = false,
)
