package tech.stit.mobile.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.observability.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Access в памяти, refresh в SecureStore; Mutex делает refresh single-flight, 401 на самом refresh — разлогин (07 §1.3b).
class SessionStore internal constructor(
    private val secureStore: SecureStore,
    private val refresher: TokenRefresher,
    private val mode: AuthMode,
    private val clock: Clock,
    private val logger: Logger,
    private val onSignedOut: suspend () -> Unit,
) {
    private class AccessToken(val value: String, val expiresAt: Instant)

    private val mutex = Mutex()
    private var access: AccessToken? = null
    private val _authState = MutableStateFlow(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val accessToken: String? get() = access?.value

    // Офлайн на старте — остаёмся SignedIn на кеше: первый удачный запрос обновит access через 401.
    suspend fun restore(): AuthState = mutex.withLock {
        val refresh = if (mode == AuthMode.Bearer) secureStore.read(REFRESH_TOKEN_KEY) else null
        if (refresh == null) {
            signOutLocked()
        } else {
            _authState.value = AuthState.SignedIn
            runCatching { refreshLocked(refresh) }.onFailure { e ->
                if (e !is ApiError) throw e
                logger.warn("restore: refresh не удался, сессия сохранена", e)
            }
        }
        _authState.value
    }

    suspend fun signIn(tokens: TokenPair?) = mutex.withLock {
        store(tokens)
        _authState.value = AuthState.SignedIn
    }

    // Токен для заголовка: истёкший access обновляется до запроса, а не после 401.
    @Throws(ApiError::class, CancellationException::class)
    suspend fun validAccessToken(): String? {
        val current = access
        return when {
            current != null && !current.isExpired() -> current.value
            mode == AuthMode.Cookie || _authState.value == AuthState.SignedOut -> null
            refreshAfter(current?.value) -> accessToken
            else -> null
        }
    }

    // true — можно повторить запрос с новым access (или обновлённой cookie); false — сессии больше нет.
    @Throws(ApiError::class, CancellationException::class)
    suspend fun refreshAfter(staleAccess: String?): Boolean = mutex.withLock {
        if (mode == AuthMode.Cookie) return refreshLocked(null)
        val current = access
        if (current != null && current.value != staleAccess && !current.isExpired()) return true
        val refresh = secureStore.read(REFRESH_TOKEN_KEY)
        if (refresh == null) {
            signOutLocked()
            return false
        }
        refreshLocked(refresh)
    }

    suspend fun clear() = mutex.withLock { signOutLocked() }

    // Смена владельца сессии (вход в demo и выход из него): access прежнего владельца забываем, кеши сущностей
    // чистим, состояние читаем заново из хранилища нового владельца — токены сторон друг друга не видят.
    internal suspend fun reset(): AuthState {
        mutex.withLock {
            access = null
            _authState.value = AuthState.Unknown
        }
        onSignedOut()
        return restore()
    }

    // Для POST /api/auth/logout: сервер отзывает refresh до локальной очистки.
    suspend fun refreshToken(): String? = if (mode == AuthMode.Bearer) secureStore.read(REFRESH_TOKEN_KEY) else null

    private suspend fun refreshLocked(refreshToken: String?): Boolean {
        val pair = try {
            refresher.refresh(refreshToken)
        } catch (e: ApiError.Http) {
            if (e.status != UNAUTHORIZED) throw e
            logger.info("refresh отклонён (${e.code}) — выход из сессии")
            signOutLocked()
            return false
        }
        store(pair)
        _authState.value = AuthState.SignedIn
        return true
    }

    private suspend fun store(tokens: TokenPair?) {
        if (tokens == null || mode == AuthMode.Cookie) return
        access = AccessToken(tokens.accessToken, clock.now() + tokens.expiresInSeconds.seconds)
        secureStore.write(REFRESH_TOKEN_KEY, tokens.refreshToken)
    }

    private suspend fun signOutLocked() {
        access = null
        secureStore.remove(REFRESH_TOKEN_KEY)
        if (_authState.value != AuthState.SignedOut) {
            _authState.value = AuthState.SignedOut
            onSignedOut()
        }
    }

    private fun AccessToken.isExpired(): Boolean = clock.now() + EXPIRY_LEEWAY >= expiresAt

    companion object {
        const val REFRESH_TOKEN_KEY = "auth.refresh_token"
        private const val UNAUTHORIZED = 401
        private val EXPIRY_LEEWAY = 30.seconds
    }
}

// Вне экранов сессию никто не восстанавливал: холодный процесс читает её сам, прежде чем решать про SignedOut.
internal suspend fun SessionStore.signedIn(): Boolean =
    (authState.value.takeIf { it != AuthState.Unknown } ?: restore()) == AuthState.SignedIn
