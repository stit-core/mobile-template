package tech.stit.mobile.feature.push

import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.core.push.PushStatus

data class PushOptInState(
    val status: PushStatus = PushStatus.NotAsked,
    val busy: Boolean = false,
    val error: MessageKey? = null,
)
