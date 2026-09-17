package tech.stit.mobile.android.feature.profile

import tech.stit.mobile.entity.user.User
import kotlin.time.Instant

// Как в mock/fixtures/populated/user.json.
internal val demoUser = User(
    id = "u-1",
    email = "demo@example.com",
    name = "Демо Пользователь",
    status = "active",
    createdAt = Instant.parse("2026-01-10T09:00:00Z"),
    updatedAt = Instant.parse("2026-09-01T12:00:00Z"),
    version = 3,
)
