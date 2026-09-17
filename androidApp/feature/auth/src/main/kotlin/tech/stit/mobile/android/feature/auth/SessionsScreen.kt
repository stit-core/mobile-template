package tech.stit.mobile.android.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.android.core.designsystem.StitIcons
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitButtonVariant
import tech.stit.mobile.android.core.designsystem.component.StitListItem
import tech.stit.mobile.android.core.designsystem.component.StitStatusBadge
import tech.stit.mobile.android.core.designsystem.component.StitTone
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.android.core.ui.EmptyState
import tech.stit.mobile.android.core.ui.ErrorState
import tech.stit.mobile.android.core.ui.LoadingState
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.android.core.ui.toDateTimeText
import tech.stit.mobile.entity.device.Device
import tech.stit.mobile.entity.session.Session
import tech.stit.mobile.feature.auth.SessionsIntent
import tech.stit.mobile.feature.auth.SessionsState

@Composable
fun SessionsScreen(state: SessionsState, onIntent: (SessionsIntent) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            StitTopBar(title = stringResource(R.string.feature_auth_sessions_title), onBack = onBack) {
                IconButton(onClick = { onIntent(SessionsIntent.Refresh) }) {
                    Icon(
                        painter = painterResource(StitIcons.Refresh),
                        contentDescription = stringResource(R.string.feature_auth_sessions_refresh),
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding).fillMaxSize()
        val error = state.error
        when {
            state.loading && state.sessions.isEmpty() -> LoadingState(modifier = content)

            error != null && state.sessions.isEmpty() ->
                ErrorState(message = messageText(error), modifier = content, onRetry = { onIntent(SessionsIntent.Refresh) })

            state.sessions.isEmpty() -> EmptyState(title = stringResource(R.string.feature_auth_sessions_empty), modifier = content)

            else -> SessionList(state = state, onIntent = onIntent, modifier = content)
        }
    }
}

@Composable
private fun SessionList(state: SessionsState, onIntent: (SessionsIntent) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        state.error?.let { error ->
            item { Text(text = messageText(error), modifier = Modifier.padding(StitTheme.spacing.md), color = StitTheme.colors.danger) }
        }
        items(state.sessions, key = {
            it.jti
        }) { session -> SessionRow(session = session, onRevoke = { onIntent(SessionsIntent.RevokeSession(it)) }) }
        if (state.devices.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.feature_auth_devices_title),
                    modifier = Modifier.padding(StitTheme.spacing.md),
                    style = StitTheme.typography.bodySm,
                    color = StitTheme.colors.muted,
                )
            }
            items(state.devices, key = { it.id }) { device ->
                DeviceRow(device = device, onUnregister = { onIntent(SessionsIntent.UnregisterDevice(it)) })
            }
        }
        item {
            Column(modifier = Modifier.padding(StitTheme.spacing.md)) {
                StitButton(
                    text = stringResource(R.string.feature_auth_sessions_revoke_all),
                    onClick = { onIntent(SessionsIntent.RevokeAll) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = StitButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session, onRevoke: (String) -> Unit, modifier: Modifier = Modifier) {
    StitListItem(
        headline = listOfNotNull(session.client, session.deviceName ?: session.appVersion).joinToString(" · "),
        modifier = modifier,
        supporting = session.lastUsedAt?.let { stringResource(R.string.feature_auth_session_last_used, it.toDateTimeText()) },
        trailing = {
            if (session.current) {
                StitStatusBadge(text = stringResource(R.string.feature_auth_session_current), tone = StitTone.Success)
            } else {
                TextButton(onClick = { onRevoke(session.jti) }) { Text(text = stringResource(R.string.feature_auth_session_revoke)) }
            }
        },
    )
}

@Composable
private fun DeviceRow(device: Device, onUnregister: (String) -> Unit, modifier: Modifier = Modifier) {
    StitListItem(
        headline = "${device.provider} · ${device.deviceModel ?: device.platform}",
        modifier = modifier,
        supporting = device.lastSeenAt.toDateTimeText(),
        trailing = {
            TextButton(onClick = { onUnregister(device.id) }) { Text(text = stringResource(R.string.feature_auth_device_unregister)) }
        },
    )
}
