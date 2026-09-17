package tech.stit.mobile.android.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import tech.stit.mobile.android.core.designsystem.generated.Tokens

@Immutable
data class StitSpacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    // Минимальная область касания 48 dp (rule design-tokens) — кнопки и строки списков не ниже.
    val touchTarget: Dp,
)

internal val StitSpacingDefault = StitSpacing(
    xs = Tokens.Dimensions.spaceXs,
    sm = Tokens.Dimensions.spaceSm,
    md = Tokens.Dimensions.spaceMd,
    lg = Tokens.Dimensions.spaceLg,
    xl = Tokens.Dimensions.spaceXl,
    touchTarget = Tokens.Dimensions.touchMin,
)
