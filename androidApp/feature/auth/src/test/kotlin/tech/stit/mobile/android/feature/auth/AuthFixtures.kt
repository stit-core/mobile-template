package tech.stit.mobile.android.feature.auth

import tech.stit.mobile.entity.device.Device
import tech.stit.mobile.entity.session.Session
import kotlin.time.Instant

// Значения — как в mock/fixtures/populated/{sessions,devices}.json: экраны в тестах видят те же данные, что demo-сборка.
internal val currentSession = Session(
    jti = "session-current",
    client = "android",
    deviceName = "Pixel 8",
    appVersion = "1.0.0+1000000",
    issuedAt = Instant.parse("2026-09-15T07:00:00Z"),
    expiresAt = Instant.parse("2026-12-14T07:00:00Z"),
    lastUsedAt = Instant.parse("2026-09-17T06:30:00Z"),
    current = true,
)

internal val webSession = Session(
    jti = "session-web",
    client = "web",
    deviceName = null,
    appVersion = "1.0.0",
    issuedAt = Instant.parse("2026-09-01T10:00:00Z"),
    expiresAt = Instant.parse("2026-10-01T10:00:00Z"),
    lastUsedAt = Instant.parse("2026-09-16T18:45:00Z"),
    current = false,
)

internal val fcmDevice = Device(
    id = "dev-1",
    provider = "fcm",
    platform = "android",
    appVersion = "1.0.0+1000000",
    deviceModel = "Pixel 8",
    createdAt = Instant.parse("2026-09-15T07:00:00Z"),
    lastSeenAt = Instant.parse("2026-09-17T06:30:00Z"),
    revokedAt = null,
)
