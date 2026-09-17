package tech.stit.mobile.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

// update_recommended не блокирует: одна строка сверху, скрывается на этот запуск.
@Composable
fun UpdateRecommendedBanner(latestVersion: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StitTheme.colors.surfaceMuted)
            .padding(start = StitTheme.spacing.md, end = StitTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.core_ui_update_recommended, latestVersion),
            modifier = Modifier.weight(1f),
            style = StitTheme.typography.bodySm,
        )
        TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.core_ui_dismiss)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateRecommendedBannerPreview() {
    StitTheme { UpdateRecommendedBanner(latestVersion = "1.2.0", onDismiss = {}) }
}
