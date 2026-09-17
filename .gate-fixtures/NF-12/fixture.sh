# NF-12 — core/** импортирует feature/**: ловит Konsist ArchitectureTest в shared jvmTest
EXPECT="shared jvm tests"
TOUCHES="shared/src/commonMain/kotlin/tech/stit/mobile/core/lib/FeatureLeak.kt"
apply() {
    cat >"$TOUCHES" <<'KT'
package tech.stit.mobile.core.lib

import tech.stit.mobile.feature.todos.TodoListState

fun leak(): TodoListState = TodoListState()
KT
}
