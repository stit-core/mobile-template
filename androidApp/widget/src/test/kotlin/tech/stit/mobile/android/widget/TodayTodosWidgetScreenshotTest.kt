package tech.stit.mobile.android.widget

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.stit.mobile.core.widget.WidgetState
import java.util.TimeZone
import kotlin.time.Duration.Companion.seconds

// Эталон на каждое состояние снимка: Glance переводит композицию в RemoteViews, вид рисуется и сравнивается.
@OptIn(ExperimentalGlanceApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class TodayTodosWidgetScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Срок в строке задачи не зависит от пояса машины, на которой записан эталон.
    @Before
    fun pinTimeZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    @Test
    fun placeholder() = capture(WidgetState.Placeholder)

    @Test
    fun populated() = capture(WidgetState.Populated)

    @Test
    fun empty() = capture(WidgetState.Empty)

    @Test
    fun signedOut() = capture(WidgetState.SignedOut)

    @Test
    fun stale() = capture(WidgetState.Stale)

    // Тёмная тема виджета — это day/night-провайдеры токенов, а не отдельный набор цветов.
    @Test
    @Config(qualifiers = "+night")
    fun populatedDark() = capture(WidgetState.Populated, darkTheme = true)

    private fun capture(state: WidgetState, darkTheme: Boolean = false) {
        val views = runBlocking {
            withTimeout(TIMEOUT) { TodayTodosWidget { snapshot(state) }.compose(context, size = SIZE) }
        }
        val density = context.resources.displayMetrics.density
        val width = (SIZE.width.value * density).toInt()
        val height = (SIZE.height.value * density).toInt()
        val view = views.apply(context, FrameLayout(context))
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        val bitmap = createBitmap(width, height)
        view.draw(Canvas(bitmap))
        val suffix = if (darkTheme) "_dark" else ""
        bitmap.captureRoboImage("src/test/screenshots/widget_today_todos_${state.fileName()}$suffix.png")
    }

    private fun WidgetState.fileName(): String = name.replace(Regex("(?<=.)([A-Z])"), "_$1").lowercase()

    private companion object {
        val SIZE = DpSize(250.dp, 200.dp)
        val TIMEOUT = 20.seconds
    }
}
