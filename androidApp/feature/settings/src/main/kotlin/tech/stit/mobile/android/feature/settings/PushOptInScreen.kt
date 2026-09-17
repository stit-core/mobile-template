package tech.stit.mobile.android.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitButtonVariant
import tech.stit.mobile.android.core.designsystem.component.StitStatusBadge
import tech.stit.mobile.android.core.designsystem.component.StitTone
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.core.push.PushStatus
import tech.stit.mobile.feature.push.PushOptInIntent
import tech.stit.mobile.feature.push.PushOptInState

// Pre-prompt перед системным диалогом POST_NOTIFICATIONS (rule deep-links-and-push): в момент ценности, не на первом экране.
@Composable
fun PushOptInScreen(state: PushOptInState, onIntent: (PushOptInIntent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier, topBar = {
        StitTopBar(title = stringResource(R.string.feature_settings_push_title), onBack = onBack)
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(StitTheme.spacing.md)) {
            Text(text = stringResource(R.string.feature_settings_push_body))
            StitStatusBadge(
                text = stringResource(state.status.label()),
                tone = if (state.status == PushStatus.Granted) StitTone.Success else StitTone.Muted,
                modifier = Modifier.padding(top = StitTheme.spacing.md),
            )
            state.error?.let { error ->
                Text(text = messageText(error), modifier = Modifier.padding(top = StitTheme.spacing.sm), color = StitTheme.colors.danger)
            }
            if (state.status == PushStatus.Granted) {
                StitButton(
                    text = stringResource(R.string.feature_settings_push_disable),
                    onClick = { onIntent(PushOptInIntent.Decline) },
                    modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.lg).testTag(SettingsTestTags.PUSH_DECLINE),
                    variant = StitButtonVariant.Secondary,
                    enabled = !state.busy,
                )
            } else {
                StitButton(
                    text = stringResource(R.string.feature_settings_push_accept),
                    onClick = { onIntent(PushOptInIntent.Accept) },
                    modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.lg).testTag(SettingsTestTags.PUSH_ACCEPT),
                    loading = state.busy,
                )
                StitButton(
                    text = stringResource(R.string.feature_settings_push_decline),
                    onClick = { onIntent(PushOptInIntent.Decline) },
                    modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.sm).testTag(SettingsTestTags.PUSH_DECLINE),
                    variant = StitButtonVariant.Secondary,
                    enabled = !state.busy,
                )
            }
        }
    }
}

private fun PushStatus.label(): Int = when (this) {
    PushStatus.NotAsked -> R.string.feature_settings_push_not_asked
    PushStatus.Granted -> R.string.feature_settings_push_granted
    PushStatus.Declined -> R.string.feature_settings_push_declined
}
