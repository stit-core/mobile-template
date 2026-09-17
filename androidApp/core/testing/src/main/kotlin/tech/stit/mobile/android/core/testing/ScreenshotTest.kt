package tech.stit.mobile.android.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.stit.mobile.android.core.designsystem.StitTheme
import java.util.TimeZone

// Форма Roborazzi-теста одна на репозиторий: эталоны — src/test/screenshots/<name>.png, перезапись — make screenshots-record.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
abstract class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(options = RoborazziRule.Options(outputDirectoryPath = SCREENSHOTS_DIR))

    // Даты в эталонах не зависят от пояса машины, на которой их записали.
    @Before
    fun pinTimeZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    protected fun capture(name: String, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { StitTheme(darkTheme = darkTheme) { content() } }
        composeRule.onRoot().captureRoboImage("$SCREENSHOTS_DIR/$name.png")
    }

    private companion object {
        const val SCREENSHOTS_DIR = "src/test/screenshots"
    }
}
