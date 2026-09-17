# NF-09 — цвет токена изменён без make screenshots-record: ловит Roborazzi verify
EXPECT="screenshots"
TOUCHES="androidApp/core/designsystem/src/main/kotlin/tech/stit/mobile/android/core/designsystem/Theme.kt"
apply() {
    sed -i 's/0xFF1B7F3B/0xFF0000FF/' "$TOUCHES"
}
