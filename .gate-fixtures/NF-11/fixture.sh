# NF-11 — android.os.Build в commonMain: ловит компиляция jvmTest (стадия kmp — второй рубеж), Konsist — третий
EXPECT="shared jvm tests"
TOUCHES="shared/src/commonMain/kotlin/tech/stit/mobile/core/lib/DeviceName.kt"
apply() {
    cat >"$TOUCHES" <<'KT'
package tech.stit.mobile.core.lib

import android.os.Build

fun deviceName(): String = Build.MODEL
KT
}
