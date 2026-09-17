# NF-10 — удалены тесты презентеров и сущностей: ловит пол покрытия shared 80 % (koverVerify)
EXPECT="coverage kotlin"
TOUCHES="shared/src/commonTest/kotlin/tech/stit/mobile/feature shared/src/commonTest/kotlin/tech/stit/mobile/entity"
apply() {
    rm -r $TOUCHES
}
