package tech.stit.mobile.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalStitColors = staticCompositionLocalOf { stitColors(darkTheme = false) }
val LocalStitTypography = staticCompositionLocalOf { StitTypographyDefault }
val LocalStitSpacing = staticCompositionLocalOf { StitSpacingDefault }
val LocalStitShapes = staticCompositionLocalOf { StitShapesDefault }

// Единственная точка доступа к токенам из композаблов: StitTheme.colors.actionBg, StitTheme.spacing.md.
object StitTheme {
    val colors: StitColors
        @Composable @ReadOnlyComposable
        get() = LocalStitColors.current

    val typography: StitTypography
        @Composable @ReadOnlyComposable
        get() = LocalStitTypography.current

    val spacing: StitSpacing
        @Composable @ReadOnlyComposable
        get() = LocalStitSpacing.current

    val shapes: StitShapes
        @Composable @ReadOnlyComposable
        get() = LocalStitShapes.current
}

@Composable
fun StitTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = stitColors(darkTheme)
    CompositionLocalProvider(
        LocalStitColors provides colors,
        LocalStitTypography provides StitTypographyDefault,
        LocalStitSpacing provides StitSpacingDefault,
        LocalStitShapes provides StitShapesDefault,
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = StitTypographyDefault.toMaterialTypography(),
            shapes = StitShapesDefault.toMaterialShapes(),
            content = content,
        )
    }
}
