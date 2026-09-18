package tech.stit.mobile.android.widget

import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import tech.stit.mobile.android.core.designsystem.generated.ColorToken
import tech.stit.mobile.android.core.designsystem.generated.Tokens

// Виджет рисует система, а не Compose-тема приложения: роли токенов раскладываются в day/night-провайдеры Glance здесь.
internal object WidgetTokens {
    val appBg = Tokens.Colors.appBg.provider()
    val appFg = Tokens.Colors.appFg.provider()
    val muted = Tokens.Colors.muted.provider()
    val success = Tokens.Colors.success.provider()

    val heading = TextStyle(color = appFg, fontSize = Tokens.Typography.bodyMd, fontWeight = FontWeight.Medium)
    val action = TextStyle(color = appFg, fontSize = Tokens.Typography.bodySm, fontWeight = FontWeight.Medium)
    val caption = TextStyle(color = muted, fontSize = Tokens.Typography.bodySm)
    val hint = TextStyle(color = muted, fontSize = Tokens.Typography.bodyMd)

    fun title(done: Boolean) = TextStyle(
        color = if (done) muted else appFg,
        fontSize = Tokens.Typography.bodySm,
        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
    )

    fun status(done: Boolean) = TextStyle(color = if (done) success else muted, fontSize = Tokens.Typography.bodySm)

    private fun ColorToken.provider() = ColorProvider(day = light, night = dark)
}
