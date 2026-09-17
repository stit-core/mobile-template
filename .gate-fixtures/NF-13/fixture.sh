# NF-13 — публичный fun <T> в *Presenter (generic в фасаде для Swift): ловит Konsist ArchitectureTest в shared jvmTest
EXPECT="shared jvm tests"
TOUCHES="shared/src/commonMain/kotlin/tech/stit/mobile/feature/todos/PickerPresenter.kt"
apply() {
    cat >"$TOUCHES" <<'KT'
package tech.stit.mobile.feature.todos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PickerPresenter {
    val state: StateFlow<TodoListState> = MutableStateFlow(TodoListState())

    fun dispatch(intent: TodoListIntent) = Unit

    fun <T> pick(items: List<T>): T? = items.firstOrNull()
}
KT
}
