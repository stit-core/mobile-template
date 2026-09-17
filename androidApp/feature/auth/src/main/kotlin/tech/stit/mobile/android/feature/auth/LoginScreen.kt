package tech.stit.mobile.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitButton
import tech.stit.mobile.android.core.designsystem.component.StitTextField
import tech.stit.mobile.android.core.ui.messageText
import tech.stit.mobile.feature.auth.LoginIntent
import tech.stit.mobile.feature.auth.LoginState

@Composable
fun LoginScreen(state: LoginState, onIntent: (LoginIntent) -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().imePadding().padding(StitTheme.spacing.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.feature_auth_login_title), style = StitTheme.typography.headingLg)
        StitTextField(
            value = state.email,
            onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
            label = stringResource(R.string.feature_auth_email),
            modifier = Modifier.padding(top = StitTheme.spacing.lg).testTag(AuthTestTags.LOGIN_EMAIL),
            enabled = !state.submitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        StitTextField(
            value = state.password,
            onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
            label = stringResource(R.string.feature_auth_password),
            modifier = Modifier.padding(top = StitTheme.spacing.sm).testTag(AuthTestTags.LOGIN_PASSWORD),
            secret = true,
            enabled = !state.submitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onIntent(LoginIntent.Submit) }),
        )
        state.error?.let { error ->
            Text(
                text = messageText(error),
                modifier = Modifier.padding(top = StitTheme.spacing.sm),
                color = StitTheme.colors.danger,
                style = StitTheme.typography.bodySm,
            )
        }
        StitButton(
            text = stringResource(R.string.feature_auth_sign_in),
            onClick = { onIntent(LoginIntent.Submit) },
            modifier = Modifier.fillMaxWidth().padding(top = StitTheme.spacing.md).testTag(AuthTestTags.LOGIN_SUBMIT),
            enabled = state.canSubmit,
            loading = state.submitting,
        )
        TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = StitTheme.spacing.sm)) {
            Text(text = stringResource(R.string.feature_auth_open_settings))
        }
    }
}
