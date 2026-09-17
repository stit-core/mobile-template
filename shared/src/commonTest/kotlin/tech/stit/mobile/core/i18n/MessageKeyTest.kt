package tech.stit.mobile.core.i18n

import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.toMessageKey
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageKeyTest {
    @Test
    fun mapsErrorsToResourceKeys() {
        val cases = listOf(
            ApiError.Http(401, "AUTH_INVALID_CREDENTIALS", null, null, emptyMap()) to "error.auth_invalid_credentials",
            ApiError.Http(422, "VALIDATION", null, null, mapOf("email" to "invalid")) to "error.validation",
            ApiError.Network(IllegalStateException("offline")) to "error.network",
            IllegalStateException("bug") to "error.unknown",
        )
        cases.forEach { (error, key) -> assertEquals(MessageKey(key), error.toMessageKey(), error.toString()) }
    }

    @Test
    fun knownCodesRegistryListsEveryConstant() {
        assertEquals(6, KnownErrorCodes.all.size)
        assertEquals(KnownErrorCodes.all.toSet(), KnownErrorCodes.all.toSet())
        assertEquals(MessageKey("error.rate_limited"), MessageKey.forErrorCode(KnownErrorCodes.RATE_LIMITED))
    }
}
