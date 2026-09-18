package tech.stit.mobile.feature.auth

import tech.stit.mobile.core.i18n.MessageKey

data class LoginState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: MessageKey? = null,
    val signedIn: Boolean = false,
    // Вход в demo на фикстурах: открыт только флагом requirements.demo_login в обычной сборке (App Review).
    val demoAvailable: Boolean = false,
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !submitting
}
