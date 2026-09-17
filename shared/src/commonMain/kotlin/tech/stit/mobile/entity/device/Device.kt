package tech.stit.mobile.entity.device

import kotlin.time.Instant

data class Device(
    val id: String,
    val provider: String,
    val platform: String,
    val appVersion: String,
    val deviceModel: String?,
    val createdAt: Instant,
    val lastSeenAt: Instant,
    val revokedAt: Instant?,
)
