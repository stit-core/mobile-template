package tech.stit.mobile.feature.profile

import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.user.User

data class ProfileState(
    val user: User? = null,
    val loading: Boolean = false,
    val error: MessageKey? = null,
    val draftName: String = "",
    val saving: Boolean = false,
    val passwordChanged: Boolean = false,
) {
    val canSaveName: Boolean get() = draftName.isNotBlank() && draftName != user?.name && !saving
}
