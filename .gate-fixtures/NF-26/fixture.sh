# NF-26 — состояние снимка виджета без эталона Roborazzi: ловит capability-check
EXPECT="capability-check"
TOUCHES="androidApp/widget/src/test/screenshots/widget_today_todos_stale.png"
apply() {
    rm -f "$TOUCHES"
}
