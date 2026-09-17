# NF-09 — цвет токена в Tokens.kt изменён без make screenshots-record: ловит Roborazzi verify
EXPECT="screenshots"
TOUCHES="androidApp/core/designsystem/src/main/kotlin/tech/stit/mobile/android/core/designsystem/generated/Tokens.kt"
apply() {
    sed -i 's/0xFF1B5E20/0xFF0000FF/' "$TOUCHES"
}
