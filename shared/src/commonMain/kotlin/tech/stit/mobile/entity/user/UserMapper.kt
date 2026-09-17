package tech.stit.mobile.entity.user

import tech.stit.mobile.core.api.generated.UserView

internal fun UserView.toModel(): User = User(
    id = id,
    email = email,
    name = name,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    // Бэкенд нумерует версии с 1, но в UserView их пока не отдаёт (B-19).
    version = version ?: 1,
)
