package tech.stit.mobile.feature.auth

import tech.stit.mobile.core.i18n.MessageKey
import tech.stit.mobile.entity.device.Device
import tech.stit.mobile.entity.session.Session

data class SessionsState(
    val loading: Boolean = false,
    val sessions: List<Session> = emptyList(),
    val devices: List<Device> = emptyList(),
    val error: MessageKey? = null,
    val signedOut: Boolean = false,
)
