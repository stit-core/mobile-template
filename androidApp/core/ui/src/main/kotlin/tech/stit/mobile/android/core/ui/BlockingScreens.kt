package tech.stit.mobile.android.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import kotlin.time.Instant

// Блокирующие оверлеи requirements (07 §4.3): версия ниже min_version и режим обслуживания закрывают приложение целиком.

@Composable
fun UpdateRequired(storeUrl: String?, onOpenStore: (String) -> Unit, modifier: Modifier = Modifier) {
    BlockingContent(
        title = stringResource(R.string.core_ui_update_required_title),
        body = stringResource(R.string.core_ui_update_required_body),
        modifier = modifier,
        actionLabel = storeUrl?.let { stringResource(R.string.core_ui_update_required_action) },
        onAction = { storeUrl?.let(onOpenStore) },
    )
}

@Composable
fun Maintenance(until: Instant?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val body = stringResource(R.string.core_ui_maintenance_body)
    val untilLine = until?.let { stringResource(R.string.core_ui_maintenance_until, it.toDateTimeText()) }
    BlockingContent(
        title = stringResource(R.string.core_ui_maintenance_title),
        body = listOfNotNull(body, untilLine).joinToString("\n"),
        modifier = modifier,
        actionLabel = stringResource(R.string.core_ui_retry),
        onAction = onRetry,
    )
}

@Composable
fun LockedState(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    BlockingContent(
        title = stringResource(R.string.core_ui_locked_title),
        body = stringResource(R.string.core_ui_locked_body),
        modifier = modifier,
        actionLabel = stringResource(R.string.core_ui_locked_action),
        onAction = onUnlock,
    )
}

@Composable
private fun BlockingContent(title: String, body: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(StitTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = StitTheme.typography.headingLg, textAlign = TextAlign.Center)
        Text(
            text = body,
            modifier = Modifier.padding(top = StitTheme.spacing.sm),
            color = StitTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            StitButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = StitTheme.spacing.md))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateRequiredPreview() {
    StitTheme { UpdateRequired(storeUrl = "https://example.com", onOpenStore = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MaintenancePreview() {
    StitTheme { Maintenance(until = null, onRetry = {}) }
}
