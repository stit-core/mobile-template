# NF-19 — токен изменён только в Tokens.kt, без design/tokens.json и Tokens.swift: ловит tokens-check
EXPECT="tokens-check"
TOUCHES="androidApp/core/designsystem/src/main/kotlin/tech/stit/mobile/android/core/designsystem/generated/Tokens.kt"
apply() {
    sed -i 's/val spaceMd: Dp = 16.dp/val spaceMd: Dp = 20.dp/' "$TOUCHES"
}
