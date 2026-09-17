# NF-10 — удалён тест общей логики: ловит пол покрытия shared 80 % (koverVerify)
EXPECT="coverage kotlin"
TOUCHES="shared/src/commonTest/kotlin/tech/stit/mobile/core/config/AppEnvTest.kt"
apply() {
    rm "$TOUCHES"
}
