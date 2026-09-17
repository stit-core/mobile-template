package tech.stit.mobile.entity.device

import tech.stit.mobile.core.api.generated.DeviceView

internal fun DeviceView.toModel(): Device = Device(
    id = id,
    provider = provider,
    platform = platform,
    appVersion = appVersion,
    deviceModel = deviceModel,
    createdAt = createdAt,
    lastSeenAt = lastSeenAt,
    revokedAt = revokedAt,
)
