package tech.stit.mobile.core.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.config.AuthMode
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.testing.FakeClock
import tech.stit.mobile.testing.FakeSecureStore
import tech.stit.mobile.testing.MemoryLogSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SessionStoreTest {
    private class FakeRefresher : TokenRefresher {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        var failure: Throwable? = null

        override suspend fun refresh(refreshToken: String?): TokenPair? {
            calls++
            gate?.await()
            failure?.let { throw it }
            return TokenPair("access-$calls", "refresh-$calls", expiresInSeconds = 900)
        }
    }

    private val secure = FakeSecureStore()
    private val refresher = FakeRefresher()
    private val clock = FakeClock()
    private var signedOutCalls = 0

    private fun store(mode: AuthMode = AuthMode.Bearer) =
        SessionStore(secure, refresher, mode, clock, Logger("test", MemoryLogSink()), onSignedOut = {
            signedOutCalls++
        })

    @Test
    fun restoreWithoutRefreshTokenIsSignedOut() = runTest {
        val store = store()
        assertEquals(AuthState.SignedOut, store.restore())
        assertEquals(0, refresher.calls)
        assertEquals(1, signedOutCalls)
    }

    @Test
    fun restoreWithRefreshTokenObtainsFreshPair() = runTest {
        secure.values[SessionStore.REFRESH_TOKEN_KEY] = "stored"
        val store = store()
        assertEquals(AuthState.SignedIn, store.restore())
        assertEquals("access-1", store.accessToken)
        assertEquals("refresh-1", secure.values[SessionStore.REFRESH_TOKEN_KEY])
    }

    @Test
    fun restoreOfflineKeepsSessionWithoutAccess() = runTest {
        secure.values[SessionStore.REFRESH_TOKEN_KEY] = "stored"
        refresher.failure = ApiError.Network(IllegalStateException("offline"))
        val store = store()
        assertEquals(AuthState.SignedIn, store.restore())
        assertNull(store.accessToken)
        assertEquals("stored", secure.values[SessionStore.REFRESH_TOKEN_KEY])
    }

    @Test
    fun rejectedRefreshSignsOutAndClearsSecrets() = runTest {
        secure.values[SessionStore.REFRESH_TOKEN_KEY] = "stored"
        refresher.failure = ApiError.Http(401, "AUTH_REFRESH_REUSED", null, null, emptyMap())
        val store = store()
        assertFalse(store.refreshAfter("stale"))
        assertEquals(AuthState.SignedOut, store.authState.value)
        assertTrue(secure.values.isEmpty())
        assertEquals(1, signedOutCalls)
    }

    @Test
    fun serverErrorOnRefreshPropagatesAndKeepsSession() = runTest {
        secure.values[SessionStore.REFRESH_TOKEN_KEY] = "stored"
        refresher.failure = ApiError.Http(503, "UNAVAILABLE", null, null, emptyMap())
        val store = store()
        store.signIn(TokenPair("access-0", "stored", 900))
        assertFailsWith<ApiError.Http> { store.refreshAfter("access-0") }
        assertEquals(AuthState.SignedIn, store.authState.value)
    }

    @Test
    fun parallelRefreshesShareOneRequest() = runTest {
        secure.values[SessionStore.REFRESH_TOKEN_KEY] = "stored"
        val store = store()
        store.signIn(TokenPair("stale", "stored", 900))
        refresher.gate = CompletableDeferred()
        val results = List(5) { async { store.refreshAfter("stale") } }
        advanceUntilIdle()
        assertEquals(1, refresher.calls)
        refresher.gate!!.complete(Unit)
        assertTrue(results.awaitAll().all { it })
        assertEquals(1, refresher.calls)
        assertEquals("access-1", store.accessToken)
    }

    @Test
    fun alreadyRefreshedTokenIsReusedWithoutNewRequest() = runTest {
        secure.values[SessionStore.REFRESH_TOKEN_KEY] = "stored"
        val store = store()
        store.signIn(TokenPair("old", "stored", 900))
        assertTrue(store.refreshAfter("old"))
        assertTrue(store.refreshAfter("old"))
        assertEquals(1, refresher.calls)
    }

    @Test
    fun expiredAccessIsRefreshedBeforeRequest() = runTest {
        val store = store()
        store.signIn(TokenPair("access-0", "refresh-0", expiresInSeconds = 60))
        assertEquals("access-0", store.validAccessToken())
        clock.current += 2.minutes
        assertEquals("access-1", store.validAccessToken())
        assertEquals(1, refresher.calls)
    }

    @Test
    fun clearForgetsEverything() = runTest {
        val store = store()
        store.signIn(TokenPair("access-0", "refresh-0", 900))
        assertEquals("refresh-0", store.refreshToken())
        store.clear()
        assertNull(store.accessToken)
        assertNull(store.refreshToken())
        assertEquals(AuthState.SignedOut, store.authState.value)
    }

    @Test
    fun cookieModeKeepsNoTokensAndRefreshesViaJar() = runTest {
        val store = store(AuthMode.Cookie)
        assertEquals(AuthState.SignedOut, store.restore())
        store.signIn(null)
        assertEquals(AuthState.SignedIn, store.authState.value)
        assertNull(store.validAccessToken())
        assertTrue(store.refreshAfter(null))
        assertEquals(1, refresher.calls)
        assertTrue(secure.values.isEmpty())
    }
}
