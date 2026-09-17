package tech.stit.mobile.entity.session

import kotlin.time.Instant

data class Session(
    val jti: String,
    val client: String?,
    val deviceName: String?,
    val appVersion: String?,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val lastUsedAt: Instant?,
    val current: Boolean,
)
