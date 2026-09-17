package tech.stit.mobile.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.core_ui_offline_banner),
        modifier = modifier
            .fillMaxWidth()
            .background(StitTheme.colors.surfaceMuted)
            .padding(horizontal = StitTheme.spacing.md, vertical = StitTheme.spacing.sm)
            .testTag(UiTestTags.OFFLINE_BANNER),
        color = StitTheme.colors.muted,
        textAlign = TextAlign.Center,
        style = StitTheme.typography.bodySm,
    )
}

@Preview(showBackground = true)
@Composable
private fun OfflineBannerPreview() {
    StitTheme { OfflineBanner() }
}
