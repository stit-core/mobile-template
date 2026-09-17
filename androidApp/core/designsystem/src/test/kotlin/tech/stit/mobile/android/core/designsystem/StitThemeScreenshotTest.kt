package tech.stit.mobile.android.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Эталоны — src/test/screenshots; перезапись только осознанно: make screenshots-record.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class StitThemeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(options = RoborazziRule.Options(outputDirectoryPath = "src/test/screenshots"))

    @Test
    fun `светлая тема`() {
        composeRule.setContent { StitTheme(darkTheme = false) { TokenSwatches() } }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/theme_light.png")
    }

    @Test
    fun `тёмная тема`() {
        composeRule.setContent { StitTheme(darkTheme = true) { TokenSwatches() } }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/theme_dark.png")
    }
}

@Composable
private fun TokenSwatches() {
    Surface {
        Column(modifier = Modifier.padding(StitTheme.spacing.md)) {
            Text(text = "mobile-template", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.padding(top = StitTheme.spacing.sm)) {
                Swatch(StitTheme.semantic.success)
                Swatch(StitTheme.semantic.warning)
                Swatch(StitTheme.semantic.muted)
            }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = StitTheme.spacing.sm)
            .size(StitTheme.spacing.xl)
            .background(color),
    )
}
