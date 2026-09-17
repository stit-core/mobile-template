# NF-04 — неиспользуемая private fun: ловит detekt UnusedPrivateMember
EXPECT="detekt"
TOUCHES="shared/src/commonMain/kotlin/tech/stit/mobile/core/lib/Dead.kt"
apply() {
    cat >"$TOUCHES" <<'KT'
package tech.stit.mobile.core.lib

class Dead {
    private fun unusedOld() = 1
}
KT
}
