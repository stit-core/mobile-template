package tech.stit.mobile.core.auth

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

// Что делает refresh: Bearer — POST /api/auth/refresh с телом → новая пара; cookie-режим — без тела, пара null, jar обновлён.
internal fun interface TokenRefresher {
    suspend fun refresh(refreshToken: String?): TokenPair?
}
