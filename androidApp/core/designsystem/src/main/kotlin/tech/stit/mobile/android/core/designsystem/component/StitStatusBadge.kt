package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

@Composable
fun StitStatusBadge(text: String, tone: StitTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        StitTone.Success -> StitTheme.colors.success
        StitTone.Warning -> StitTheme.colors.warning
        StitTone.Danger -> StitTheme.colors.danger
        StitTone.Muted -> StitTheme.colors.muted
    }
    Text(
        text = text,
        modifier = modifier
            .background(color = color.copy(alpha = BADGE_BACKGROUND_ALPHA), shape = StitTheme.shapes.control)
            .padding(horizontal = StitTheme.spacing.sm, vertical = StitTheme.spacing.xs),
        color = color,
        style = StitTheme.typography.bodySm,
    )
}

// Подложка бейджа — тот же цвет с прозрачностью: контраст текста сохраняется в обеих темах.
private const val BADGE_BACKGROUND_ALPHA = 0.12f

@Preview(showBackground = true)
@Composable
private fun StitStatusBadgePreview() {
    StitTheme {
        Row(modifier = Modifier.padding(StitTheme.spacing.md)) {
            StitStatusBadge(text = "Success", tone = StitTone.Success)
            StitStatusBadge(text = "Warning", tone = StitTone.Warning, modifier = Modifier.padding(start = StitTheme.spacing.sm))
            StitStatusBadge(text = "Danger", tone = StitTone.Danger, modifier = Modifier.padding(start = StitTheme.spacing.sm))
        }
    }
}
