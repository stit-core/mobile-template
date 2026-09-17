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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitButtonVariant

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(StitTheme.spacing.lg).testTag(UiTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = StitTheme.typography.headingLg, textAlign = TextAlign.Center)
        if (description != null) {
            Text(
                text = description,
                modifier = Modifier.padding(top = StitTheme.spacing.sm),
                color = StitTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null) {
            StitButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = StitTheme.spacing.md),
                variant = StitButtonVariant.Secondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    StitTheme { EmptyState(title = "Пока пусто", description = "Добавьте первую задачу", actionLabel = "Добавить") }
}
