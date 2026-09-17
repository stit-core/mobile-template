# NF-06 — сырой 0xFF… в композабле вместо токена StitTheme: ловит detekt MagicNumber
EXPECT="detekt"
TOUCHES="androidApp/core/ui/src/main/kotlin/tech/stit/mobile/android/core/ui/RawColor.kt"
apply() {
    mkdir -p "$(dirname "$TOUCHES")"
    cat >"$TOUCHES" <<'KT'
package tech.stit.mobile.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun RawColorBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF112233)))
}
KT
}
