package tech.stit.mobile.android.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.feature.auth.DeleteAccountIntent
import tech.stit.mobile.feature.auth.DeleteAccountState

// Обязательный экран (App Store 5.1.1(v), rule auth-and-session): явное подтверждение → DELETE → локальный выход.
@Composable
fun DeleteAccountScreen(
    state: DeleteAccountState,
    onIntent: (DeleteAccountIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = {
        StitTopBar(title = stringResource(R.string.feature_auth_delete_title), onBack = onBack)
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(StitTheme.spacing.md)) {
            Text(text = stringResource(R.string.feature_auth_delete_body))
            Row(modifier = Modifier.padding(top = StitTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.confirmed,
                    onCheckedChange = { onIntent(DeleteAccountIntent.ConfirmChanged(it)) },
                    modifier = Modifier.testTag(AuthTestTags.DELETE_CONFIRM),
                    enabled = !state.deleting,
                )
                Text(text = stringResource(R.string.feature_auth_delete_confirm), style = StitTheme.typography.bodySm)
            }
            state.error?.let { error ->
                Text(text = messageText(error), modifier = Modifier.padding(top = StitTheme.spacing.sm), color = StitTheme.colors.danger)
            }
            if (state.deleted) {
                Text(text = stringResource(R.string.feature_auth_deleted), modifier = Modifier.padding(top = StitTheme.spacing.sm))
            }
            StitButton(
                text = stringResource(R.string.feature_auth_delete_action),
                onClick = { onIntent(DeleteAccountIntent.Delete) },
                modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.md).testTag(AuthTestTags.DELETE_SUBMIT),
                enabled = state.canDelete,
                loading = state.deleting,
            )
        }
    }
}
