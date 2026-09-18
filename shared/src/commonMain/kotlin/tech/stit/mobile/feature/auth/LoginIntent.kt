package tech.stit.mobile.feature.auth

sealed class LoginIntent {
    data object Start : LoginIntent()

    data class EmailChanged(val value: String) : LoginIntent()

    data class PasswordChanged(val value: String) : LoginIntent()

    data object Submit : LoginIntent()

    data object EnterDemo : LoginIntent()
}
