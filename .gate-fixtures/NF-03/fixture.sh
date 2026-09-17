# NF-03 — строка не по ktlint: ловит Spotless (spotlessCheck)
EXPECT="format kotlin"
TOUCHES="shared/src/commonMain/kotlin/tech/stit/mobile/core/lib/Unformatted.kt"
apply() {
    printf 'package tech.stit.mobile.core.lib\n\nfun   unformatted( ) :  Int  {  return 1 }\n' >"$TOUCHES"
}
