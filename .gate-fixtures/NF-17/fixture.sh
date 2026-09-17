# NF-17 — строка добавлена только в values/, без values-ru/: ловит i18n-check
EXPECT="i18n-check"
TOUCHES="androidApp/feature/todos/src/main/res/values/strings.xml"
apply() {
    sed -i 's|</resources>|    <string name="feature_todos_only_english">Only in English</string>\n</resources>|' "$TOUCHES"
}
