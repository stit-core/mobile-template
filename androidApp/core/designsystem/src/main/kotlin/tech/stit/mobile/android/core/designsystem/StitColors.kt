package tech.stit.mobile.android.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import tech.stit.mobile.android.core.designsystem.generated.ColorToken
import tech.stit.mobile.android.core.designsystem.generated.Tokens

// Семантические роли цвета (rule design-tokens): экран знает роль, а не палитру; значения — из design/tokens.json.
@Immutable
data class StitColors(
    val appBg: Color,
    val appFg: Color,
    val surfaceMuted: Color,
    val divider: Color,
    val actionBg: Color,
    val actionFg: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val muted: Color,
)

internal fun stitColors(darkTheme: Boolean): StitColors = StitColors(
    appBg = Tokens.Colors.appBg.pick(darkTheme),
    appFg = Tokens.Colors.appFg.pick(darkTheme),
    surfaceMuted = Tokens.Colors.surfaceMuted.pick(darkTheme),
    divider = Tokens.Colors.divider.pick(darkTheme),
    actionBg = Tokens.Colors.actionBg.pick(darkTheme),
    actionFg = Tokens.Colors.actionFg.pick(darkTheme),
    danger = Tokens.Colors.danger.pick(darkTheme),
    success = Tokens.Colors.success.pick(darkTheme),
    warning = Tokens.Colors.warning.pick(darkTheme),
    muted = Tokens.Colors.muted.pick(darkTheme),
)

// Material-компоненты читают роли из ColorScheme: один маппинг, dynamic color выключен (бренд и детерминизм скриншотов).
internal fun StitColors.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = actionBg,
        onPrimary = actionFg,
        background = appBg,
        onBackground = appFg,
        surface = appBg,
        onSurface = appFg,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = muted,
        surfaceContainer = surfaceMuted,
        surfaceContainerLow = surfaceMuted,
        surfaceContainerHigh = surfaceMuted,
        surfaceContainerHighest = surfaceMuted,
        outline = divider,
        outlineVariant = divider,
        error = danger,
    )
}

private fun ColorToken.pick(darkTheme: Boolean): Color = if (darkTheme) dark else light
