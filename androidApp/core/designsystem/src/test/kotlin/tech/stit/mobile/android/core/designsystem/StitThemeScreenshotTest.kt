package tech.stit.mobile.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest

// Образец для каталога (rule design-tokens): смена токена меняет эталон — перезапись только make screenshots-record.
class StitThemeScreenshotTest : ScreenshotTest() {
    @Test
    fun `светлая тема`() = capture("theme_light", darkTheme = false) { TokenSwatches() }

    @Test
    fun `тёмная тема`() = capture("theme_dark", darkTheme = true) { TokenSwatches() }
}

@Composable
private fun TokenSwatches() {
    val colors = StitTheme.colors
    val swatches = listOf(
        "appBg" to colors.appBg,
        "appFg" to colors.appFg,
        "surfaceMuted" to colors.surfaceMuted,
        "divider" to colors.divider,
        "actionBg" to colors.actionBg,
        "actionFg" to colors.actionFg,
        "danger" to colors.danger,
        "success" to colors.success,
        "warning" to colors.warning,
        "muted" to colors.muted,
    )
    Surface {
        Column(modifier = Modifier.fillMaxWidth().padding(StitTheme.spacing.md)) {
            Text(text = "Heading lg", style = StitTheme.typography.headingLg)
            Text(text = "Body md — основной текст", style = StitTheme.typography.bodyMd)
            Text(text = "Body sm — подпись", style = StitTheme.typography.bodySm, color = colors.muted)
            swatches.forEach { (name, color) -> Swatch(name = name, color = color) }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Row(modifier = Modifier.padding(top = StitTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .size(StitTheme.spacing.lg)
                .background(color = StitTheme.colors.divider, shape = StitTheme.shapes.control)
                .padding(1.dp)
                .background(color = color, shape = StitTheme.shapes.control),
        ) {}
        Text(text = name, modifier = Modifier.padding(start = StitTheme.spacing.sm), style = StitTheme.typography.bodySm)
    }
}
