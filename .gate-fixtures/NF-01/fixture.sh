# NF-01 — запрещённый импорт android.util.Log: ловит detekt ForbiddenImport
EXPECT="detekt"
TOUCHES="androidApp/core/ui/src/main/kotlin/tech/stit/mobile/android/core/ui/BadLogging.kt"
apply() {
    mkdir -p "$(dirname "$TOUCHES")"
    cat >"$TOUCHES" <<'KT'
package tech.stit.mobile.android.core.ui

import android.util.Log

fun logSomething() {
    Log.d("stit", "forbidden")
}
KT
}
