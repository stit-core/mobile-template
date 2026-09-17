package tech.stit.mobile.feature.auth

import tech.stit.mobile.core.i18n.MessageKey

data class DeleteAccountState(
    val confirmed: Boolean = false,
    val deleting: Boolean = false,
    val error: MessageKey? = null,
    val deleted: Boolean = false,
) {
    val canDelete: Boolean get() = confirmed && !deleting
}
