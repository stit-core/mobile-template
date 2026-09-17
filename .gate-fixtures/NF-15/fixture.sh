# NF-15 — правка сгенерированной модели руками: ловит gen-check (sha дерева ≠ api/openapi.lock)
EXPECT="gen-check"
TOUCHES="shared/src/commonMain/kotlin/tech/stit/mobile/core/api/generated/TodoView.kt"
apply() {
    sed -i 's/@SerialName(value = "done") @Required val done: kotlin.Boolean/@SerialName(value = "done") val done: kotlin.Boolean = false/' "$TOUCHES"
}
