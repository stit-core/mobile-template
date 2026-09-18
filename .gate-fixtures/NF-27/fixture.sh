# NF-27 — ответ системного действия без русской локали (строка только в values/): ловит capability-check
EXPECT="capability-check"
TOUCHES="androidApp/core/platform/src/main/res/values-ru/strings.xml"
apply() {
    sed -i '/core_platform_action_complete_todo_signed_out/d' "$TOUCHES"
}
