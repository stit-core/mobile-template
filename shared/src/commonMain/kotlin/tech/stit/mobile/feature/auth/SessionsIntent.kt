package tech.stit.mobile.feature.auth

sealed class SessionsIntent {
    data object Refresh : SessionsIntent()

    data class RevokeSession(val jti: String) : SessionsIntent()

    data object RevokeAll : SessionsIntent()

    data class UnregisterDevice(val id: String) : SessionsIntent()
}
