package tech.stit.mobile.feature.todos

sealed class TodoDetailIntent {
    data object Start : TodoDetailIntent()

    data object Toggle : TodoDetailIntent()

    data object Delete : TodoDetailIntent()
}
