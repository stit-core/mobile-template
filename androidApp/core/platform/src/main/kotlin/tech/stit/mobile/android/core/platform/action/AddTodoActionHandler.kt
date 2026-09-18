package tech.stit.mobile.android.core.platform.action

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.R
import tech.stit.mobile.feature.todos.AddTodoResult
import javax.inject.Inject
import javax.inject.Singleton

// Вход голоса и автоматизаций: shared решает, платформа переводит результат в короткий ответ ассистента.
@Singleton
class AddTodoActionHandler @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val container: SharedContainer,
) {
    suspend fun add(text: String): String = when (val result = container.addTodoAction().run(text)) {
        is AddTodoResult.Added -> context.getString(R.string.core_platform_action_add_todo_added, result.title)
        AddTodoResult.SignedOut -> context.getString(R.string.core_platform_action_add_todo_signed_out)
        is AddTodoResult.Failed -> context.getString(R.string.core_platform_action_add_todo_failed)
    }
}
