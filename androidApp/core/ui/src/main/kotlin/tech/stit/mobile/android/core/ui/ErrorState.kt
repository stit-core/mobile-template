package tech.stit.mobile.android.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitButtonVariant

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(StitTheme.spacing.lg).testTag(UiTestTags.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = StitTheme.colors.danger, textAlign = TextAlign.Center)
        if (onRetry != null) {
            StitButton(
                text = stringResource(R.string.core_ui_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = StitTheme.spacing.md),
                variant = StitButtonVariant.Secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    StitTheme { ErrorState(message = "Ошибка сервера. Попробуйте позже.", onRetry = {}) }
}
