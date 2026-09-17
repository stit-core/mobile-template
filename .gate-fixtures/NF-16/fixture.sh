# NF-16 — поле не по схеме в фикстуре: ловит FixturesTest (строгий разбор сгенерированными моделями) в mock-check
EXPECT="mock-check"
TOUCHES="mock/fixtures/populated/todos.json"
apply() {
    sed -i '0,/"id": "todo-1",/s//"id": "todo-1",\n      "priority": "high",/' "$TOUCHES"
}
