package tech.stit.mobile.entity.todo

import kotlin.time.Instant

data class Todo(
    val id: String,
    val title: String,
    val done: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)
