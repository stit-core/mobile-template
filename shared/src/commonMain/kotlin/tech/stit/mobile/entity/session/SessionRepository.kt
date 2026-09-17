package tech.stit.mobile.entity.session

import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.core.api.AuthApi
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.auth.TokenPair
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.core.push.PushRegistrar

// Вход, выход и список сессий пользователя; токены хранит SessionStore, здесь — порядок действий.
class SessionRepository internal constructor(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
    private val pushRegistrar: PushRegistrar,
    private val authMode: AuthMode,
    private val logger: Logger,
) {
    val authState: StateFlow<AuthState> get() = sessionStore.authState

    suspend fun login(email: String, password: String) {
        val tokens = api.login(email, password).tokens?.let { TokenPair(it.accessToken, it.refreshToken, it.expiresIn) }
        check(authMode == AuthMode.Cookie || tokens != null) { "бэкенд не вернул tokens: нужны B-1…B-3 на бэкенде или AUTH_MODE=cookie" }
        sessionStore.signIn(tokens)
    }

    // Устройство отвязывается и refresh отзывается, пока access ещё валиден; сбой сервера не мешает выйти локально.
    suspend fun logout() {
        pushRegistrar.unregister()
        runCatching { api.logout(sessionStore.refreshToken()) }.onFailure { logger.warn("logout на сервере не удался", it) }
        sessionStore.clear()
    }

    suspend fun sessions(): List<Session> = api.sessions().data.map { it.toModel() }

    suspend fun revoke(jti: String) = api.revokeSession(jti)

    // «Выйти со всех устройств» отзывает и текущую сессию.
    suspend fun revokeAll() {
        api.revokeAllSessions()
        sessionStore.clear()
    }
}
