# NF-07 — опечатка в ключе config/detekt/detekt.yml: ловит detekt config.validation
EXPECT="detekt"
TOUCHES="config/detekt/detekt.yml"
apply() {
    sed -i 's/^  MaxLineLength:/  MaxLineLenght:/' "$TOUCHES"
}
