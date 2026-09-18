# NF-25 — фоновая задача shared без идентификатора в BGTaskSchedulerPermittedIdentifiers: ловит capability-check
EXPECT="capability-check"
TOUCHES="iosApp/App/Info.plist"
apply() {
    sed -i '\|<string>\$(BUNDLE_ID)\.refresh-todos</string>|d' "$TOUCHES"
}
