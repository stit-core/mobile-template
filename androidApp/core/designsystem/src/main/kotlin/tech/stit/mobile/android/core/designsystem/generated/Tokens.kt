package tech.stit.mobile.android.core.designsystem.generated

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Сгенерировано из design/tokens.json командой make gen-tokens — руками не правится, дрейф ловит tokens-check.
class ColorToken(val light: Color, val dark: Color)

object Tokens {
    object Colors {
        val appBg: ColorToken = ColorToken(light = Color(0xFFFFFFFF), dark = Color(0xFF121212))
        val appFg: ColorToken = ColorToken(light = Color(0xFF1A1A1A), dark = Color(0xFFECECEC))
        val surfaceMuted: ColorToken = ColorToken(light = Color(0xFFF2F2F2), dark = Color(0xFF1E1E1E))
        val divider: ColorToken = ColorToken(light = Color(0xFFE0E0E0), dark = Color(0xFF2E2E2E))
        val actionBg: ColorToken = ColorToken(light = Color(0xFF1B5E20), dark = Color(0xFF81C784))
        val actionFg: ColorToken = ColorToken(light = Color(0xFFFFFFFF), dark = Color(0xFF0B2E10))
        val danger: ColorToken = ColorToken(light = Color(0xFFB3261E), dark = Color(0xFFF2B8B5))
        val success: ColorToken = ColorToken(light = Color(0xFF1B7F3B), dark = Color(0xFF6ED08C))
        val warning: ColorToken = ColorToken(light = Color(0xFF8A5200), dark = Color(0xFFFFC46B))
        val muted: ColorToken = ColorToken(light = Color(0xFF6B6B6B), dark = Color(0xFF9A9A9A))
    }

    object Dimensions {
        val spaceXs: Dp = 4.dp
        val spaceSm: Dp = 8.dp
        val spaceMd: Dp = 16.dp
        val spaceLg: Dp = 24.dp
        val spaceXl: Dp = 32.dp
        val radiusControl: Dp = 12.dp
        val radiusCard: Dp = 16.dp
        val touchMin: Dp = 48.dp
    }

    object Typography {
        val bodySm: TextUnit = 14.sp
        val bodyMd: TextUnit = 16.sp
        val headingLg: TextUnit = 22.sp
    }
}
