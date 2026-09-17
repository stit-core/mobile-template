package tech.stit.mobile.feature.auth

sealed class DeleteAccountIntent {
    data class ConfirmChanged(val confirmed: Boolean) : DeleteAccountIntent()

    data object Delete : DeleteAccountIntent()
}
