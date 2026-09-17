package tech.stit.mobile.entity.session

import tech.stit.mobile.core.api.generated.SessionView

internal fun SessionView.toModel(): Session = Session(
    jti = jti,
    client = client?.value,
    deviceName = deviceName,
    appVersion = appVersion,
    issuedAt = issuedAt,
    expiresAt = expiresAt,
    lastUsedAt = lastUsedAt,
    current = current,
)
