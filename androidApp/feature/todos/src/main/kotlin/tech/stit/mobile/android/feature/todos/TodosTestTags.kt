package tech.stit.mobile.android.feature.todos

object TodosTestTags {
    const val NEW_TITLE = "todo_new_title"
    const val ADD = "todo_add"
    const val LIST = "todo_list"
    const val TOGGLE = "todo_toggle"
    const val DELETE = "todo_delete"

    fun item(id: String): String = "todo_$id"
}
