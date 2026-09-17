package tech.stit.mobile.feature.profile

sealed class ProfileIntent {
    data object Start : ProfileIntent()

    data object Refresh : ProfileIntent()

    data class NameChanged(val value: String) : ProfileIntent()

    data object SaveName : ProfileIntent()

    data class ChangePassword(val oldPassword: String, val newPassword: String) : ProfileIntent()

    data object Logout : ProfileIntent()
}
