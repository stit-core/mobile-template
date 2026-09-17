package tech.stit.mobile.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class DesignSystemDetectorTest {
    private val composeStubs = arrayOf(
        kotlin(
            """
            package androidx.compose.ui.graphics
            class Color(val value: ULong)
            fun Color(color: Long): Color = Color(color.toULong())
            """,
        ).indented(),
        kotlin(
            "src/androidx/compose/ui/unit/Dp.kt",
            """
            package androidx.compose.ui.unit
            class Dp(val value: Float)
            val Int.dp: Dp get() = Dp(toFloat())
            """,
        ).indented(),
        kotlin(
            "src/androidx/compose/ui/unit/TextUnit.kt",
            """
            package androidx.compose.ui.unit
            class TextUnit(val value: Float)
            val Int.sp: TextUnit get() = TextUnit(toFloat())
            """,
        ).indented(),
    )

    @Test
    fun `сырые цвет и размеры в feature-модуле — ошибки, включая property-декларации`() {
        lint()
            .files(
                *composeStubs,
                kotlin(
                    """
                    package tech.stit.mobile.android.feature.todos
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.unit.dp
                    import androidx.compose.ui.unit.sp
                    private val Accent = Color(0xFF112233)
                    fun sizes() = listOf(16.dp, 14.sp, (-4).dp)
                    """,
                ).indented(),
            )
            .issues(DesignSystemDetector.ISSUE)
            .run()
            .expectErrorCount(4)
            .expectContains("Сырой Color(…) вне core:designsystem")
            // Lint печатает сообщения без обратных кавычек.
            .expectContains("Сырой размер .dp/.sp вне core:designsystem")
    }

    @Test
    fun `в core designsystem и без литералов нарушений нет`() {
        lint()
            .files(
                *composeStubs,
                kotlin(
                    """
                    package tech.stit.mobile.android.core.designsystem.generated
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.unit.dp
                    object Tokens {
                        val appBg = Color(0xFFFFFFFF)
                        val spaceMd = 16.dp
                    }
                    """,
                ).indented(),
                kotlin(
                    """
                    package tech.stit.mobile.android.feature.todos
                    import androidx.compose.ui.unit.dp
                    fun scaled(base: Int) = base.dp
                    """,
                ).indented(),
            )
            .issues(DesignSystemDetector.ISSUE)
            .run()
            .expectClean()
    }
}
