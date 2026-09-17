package tech.stit.mobile.android.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import tech.stit.mobile.core.i18n.KnownErrorCodes
import tech.stit.mobile.core.i18n.MessageKey

// error.<code> из shared → строковый ресурс (07 §2.6): код — единственный ключ локализации, неизвестный → общая формулировка.
@Composable
@ReadOnlyComposable
fun messageText(key: MessageKey): String = stringResource(key.stringRes())

@StringRes
fun MessageKey.stringRes(): Int = MESSAGE_RESOURCES[value] ?: R.string.core_ui_error_unknown

private val MESSAGE_RESOURCES: Map<String, Int> = mapOf(
    MessageKey.NetworkError.value to R.string.core_ui_error_network,
    MessageKey.UnknownError.value to R.string.core_ui_error_unknown,
    MessageKey.ValidationError.value to R.string.core_ui_error_validation,
    MessageKey.forErrorCode(KnownErrorCodes.AUTH_INVALID_CREDENTIALS).value to R.string.core_ui_error_auth_invalid_credentials,
    MessageKey.forErrorCode(KnownErrorCodes.AUTH_REFRESH_EXPIRED).value to R.string.core_ui_error_auth_refresh_expired,
    MessageKey.forErrorCode(KnownErrorCodes.AUTH_REFRESH_REUSED).value to R.string.core_ui_error_auth_refresh_reused,
    MessageKey.forErrorCode(KnownErrorCodes.AUTH_ACCOUNT_LOCKED).value to R.string.core_ui_error_auth_account_locked,
    MessageKey.forErrorCode(KnownErrorCodes.RATE_LIMITED).value to R.string.core_ui_error_rate_limited,
    MessageKey.forErrorCode(KnownErrorCodes.INTERNAL).value to R.string.core_ui_error_internal,
)
