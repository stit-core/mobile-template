package tech.stit.mobile.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Собственный токен-слой поверх Material 3: экраны знают семантику проекта, а не сырые значения.
@Immutable
data class StitSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

@Immutable
data class StitSemanticColors(
    val success: Color,
    val warning: Color,
    val muted: Color,
)

val LocalStitSpacing = staticCompositionLocalOf { StitSpacing() }
val LocalStitColors = staticCompositionLocalOf { LightSemantic }

object StitTheme {
    val spacing: StitSpacing
        @Composable get() = LocalStitSpacing.current

    val semantic: StitSemanticColors
        @Composable get() = LocalStitColors.current
}

@Composable
fun StitTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStitSpacing provides StitSpacing(),
        LocalStitColors provides if (darkTheme) DarkSemantic else LightSemantic,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}

private val LightSemantic = StitSemanticColors(
    success = Color(0xFF1B7F3B),
    warning = Color(0xFFB26A00),
    muted = Color(0xFF6B6B6B),
)

private val DarkSemantic = StitSemanticColors(
    success = Color(0xFF6ED08C),
    warning = Color(0xFFFFC46B),
    muted = Color(0xFF9A9A9A),
)
