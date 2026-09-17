package tech.stit.mobile.feature.todos

sealed class TodoListIntent {
    data object Start : TodoListIntent()

    data object Refresh : TodoListIntent()

    data class TitleChanged(val value: String) : TodoListIntent()

    data object Create : TodoListIntent()

    data class Toggle(val id: String) : TodoListIntent()

    data class Delete(val id: String) : TodoListIntent()
}
