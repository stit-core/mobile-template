package tech.stit.mobile.feature.auth

import tech.stit.mobile.core.i18n.MessageKey

data class LoginState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: MessageKey? = null,
    val signedIn: Boolean = false,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !submitting
}
