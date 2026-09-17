package tech.stit.mobile.android.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.android.core.designsystem.StitIcons
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitButtonVariant
import tech.stit.mobile.android.core.designsystem.component.StitListItem
import tech.stit.mobile.android.core.designsystem.component.StitStatusBadge
import tech.stit.mobile.android.core.designsystem.component.StitTextField
import tech.stit.mobile.android.core.designsystem.component.StitTone
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.android.core.ui.ErrorState
import tech.stit.mobile.android.core.ui.LoadingState
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.entity.user.User
import tech.stit.mobile.feature.profile.ProfileIntent
import tech.stit.mobile.feature.profile.ProfileState

@Composable
fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onOpenSessions: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            StitTopBar(title = stringResource(R.string.feature_profile_title)) {
                IconButton(onClick = { onIntent(ProfileIntent.Refresh) }) {
                    Icon(
                        painter = painterResource(StitIcons.Refresh),
                        contentDescription = stringResource(R.string.feature_profile_refresh),
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding).fillMaxSize()
        val user = state.user
        val error = state.error
        when {
            user != null -> ProfileContent(
                user = user,
                state = state,
                onIntent = onIntent,
                onOpenSessions = onOpenSessions,
                onOpenDeleteAccount = onOpenDeleteAccount,
                modifier = content,
            )

            error != null -> ErrorState(message = messageText(error), modifier = content, onRetry = { onIntent(ProfileIntent.Refresh) })

            else -> LoadingState(modifier = content)
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onOpenSessions: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(StitTheme.spacing.md)) {
        Text(text = user.email, color = StitTheme.colors.muted, style = StitTheme.typography.bodySm)
        StitTextField(
            value = state.draftName,
            onValueChange = { onIntent(ProfileIntent.NameChanged(it)) },
            label = stringResource(R.string.feature_profile_name),
            modifier = Modifier.padding(top = StitTheme.spacing.sm).testTag(ProfileTestTags.NAME),
            enabled = !state.saving,
        )
        state.error?.let { error ->
            Text(text = messageText(error), modifier = Modifier.padding(top = StitTheme.spacing.sm), color = StitTheme.colors.danger)
        }
        StitButton(
            text = stringResource(R.string.feature_profile_save),
            onClick = { onIntent(ProfileIntent.SaveName) },
            modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.sm).testTag(ProfileTestTags.SAVE),
            enabled = state.canSaveName,
            loading = state.saving,
        )
        PasswordSection(state = state, onIntent = onIntent, modifier = Modifier.padding(top = StitTheme.spacing.lg))
        StitListItem(headline = stringResource(R.string.feature_profile_sessions), onClick = onOpenSessions)
        StitListItem(headline = stringResource(R.string.feature_profile_delete_account), onClick = onOpenDeleteAccount)
        StitButton(
            text = stringResource(R.string.feature_profile_logout),
            onClick = { onIntent(ProfileIntent.Logout) },
            modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.md),
            variant = StitButtonVariant.Secondary,
        )
    }
}

// Пароли — локальное состояние экрана: презентер получает пару одним интентом и не хранит секреты в State.
@Composable
private fun PasswordSection(state: ProfileState, onIntent: (ProfileIntent) -> Unit, modifier: Modifier = Modifier) {
    var oldPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    Column(modifier = modifier) {
        Text(text = stringResource(R.string.feature_profile_password_section), style = StitTheme.typography.headingLg)
        StitTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = stringResource(R.string.feature_profile_old_password),
            modifier = Modifier.padding(top = StitTheme.spacing.sm).testTag(ProfileTestTags.OLD_PASSWORD),
            secret = true,
        )
        StitTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = stringResource(R.string.feature_profile_new_password),
            modifier = Modifier.padding(top = StitTheme.spacing.sm).testTag(ProfileTestTags.NEW_PASSWORD),
            secret = true,
        )
        if (state.passwordChanged) {
            StitStatusBadge(
                text = stringResource(R.string.feature_profile_password_changed),
                tone = StitTone.Success,
                modifier = Modifier.padding(top = StitTheme.spacing.sm),
            )
        }
        StitButton(
            text = stringResource(R.string.feature_profile_change_password),
            onClick = {
                onIntent(ProfileIntent.ChangePassword(oldPassword, newPassword))
                oldPassword = ""
                newPassword = ""
            },
            modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.sm).testTag(ProfileTestTags.CHANGE_PASSWORD),
            variant = StitButtonVariant.Secondary,
            enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && !state.saving,
        )
    }
}
