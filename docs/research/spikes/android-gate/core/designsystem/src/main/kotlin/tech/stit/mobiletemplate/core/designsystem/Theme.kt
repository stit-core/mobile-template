package tech.stit.mobiletemplate.core.designsystem

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

/** Собственный токен-слой поверх Material 3: семантика проекта, а не сырые значения. */
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
    val doneText: Color,
)

val LocalStitSpacing = staticCompositionLocalOf { StitSpacing() }

private val SuccessLight = Color(0xFF1B7F3B)
private val WarningLight = Color(0xFFB26A00)
private val DoneTextLight = Color(0xFF6B6B6B)
private val SuccessDark = Color(0xFF6ED08C)
private val WarningDark = Color(0xFFFFC46B)
private val DoneTextDark = Color(0xFF9A9A9A)

private val LightSemantic = StitSemanticColors(SuccessLight, WarningLight, DoneTextLight)
private val DarkSemantic = StitSemanticColors(SuccessDark, WarningDark, DoneTextDark)

val LocalStitColors = staticCompositionLocalOf { LightSemantic }

object StitTheme {
    val spacing: StitSpacing
        @Composable get() = LocalStitSpacing.current

    val semantic: StitSemanticColors
        @Composable get() = LocalStitColors.current
}

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun StitTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val semantic = if (darkTheme) DarkSemantic else LightSemantic
    CompositionLocalProvider(
        LocalStitSpacing provides StitSpacing(),
        LocalStitColors provides semantic,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
