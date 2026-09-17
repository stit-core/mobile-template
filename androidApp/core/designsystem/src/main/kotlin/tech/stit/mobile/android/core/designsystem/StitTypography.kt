package tech.stit.mobile.android.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import tech.stit.mobile.android.core.designsystem.generated.Tokens

@Immutable
data class StitTypography(
    val bodySm: TextStyle,
    val bodyMd: TextStyle,
    val headingLg: TextStyle,
)

// Только sp из токенов: fontScale пользователя масштабирует текст, скриншот-вариант проверяет 1.3.
internal val StitTypographyDefault = StitTypography(
    bodySm = TextStyle(fontSize = Tokens.Typography.bodySm, lineHeight = Tokens.Typography.bodySm * LINE_HEIGHT),
    bodyMd = TextStyle(fontSize = Tokens.Typography.bodyMd, lineHeight = Tokens.Typography.bodyMd * LINE_HEIGHT),
    headingLg = TextStyle(
        fontSize = Tokens.Typography.headingLg,
        lineHeight = Tokens.Typography.headingLg * LINE_HEIGHT,
        fontWeight = FontWeight.SemiBold,
    ),
)

internal fun StitTypography.toMaterialTypography(): Typography = Typography().copy(
    bodySmall = bodySm,
    bodyMedium = bodyMd,
    bodyLarge = bodyMd,
    labelLarge = bodyMd,
    titleLarge = headingLg,
    headlineSmall = headingLg,
)

private const val LINE_HEIGHT = 1.4f
