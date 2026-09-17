package tech.stit.mobile.entity.user

import kotlin.time.Instant

data class User(
    val id: String,
    val email: String,
    val name: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)
