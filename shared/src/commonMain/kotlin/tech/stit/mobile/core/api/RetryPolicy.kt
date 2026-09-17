package tech.stit.mobile.core.api

import io.ktor.http.HttpMethod
import kotlin.random.Random

// 07 §2.6: 408/429/502/503/504 и обрывы сети; Retry-After с потолком 5 минут, иначе backoff с jitter.
internal object RetryPolicy {
    const val MAX_RETRIES = 3
    val retryableStatuses: Set<Int> = setOf(408, 429, 502, 503, 504)

    private const val BASE_DELAY_MS = 1_000L
    private const val JITTER_MS = 500L
    private const val MAX_DELAY_MS = 300_000L
    private const val MAX_EXPONENT = 8
    private const val MS_PER_SECOND = 1_000L

    fun delayMs(attempt: Int, retryAfterHeader: String?, random: Random = Random.Default): Long {
        val retryAfter = retryAfterHeader?.trim()?.toLongOrNull()?.let { it * MS_PER_SECOND }
        val backoff = BASE_DELAY_MS * (1L shl (attempt - 1).coerceIn(0, MAX_EXPONENT)) + random.nextLong(JITTER_MS)
        return (retryAfter ?: backoff).coerceIn(0, MAX_DELAY_MS)
    }

    fun isRepeatable(method: HttpMethod, hasIdempotencyKey: Boolean): Boolean =
        hasIdempotencyKey || (method != HttpMethod.Post && method != HttpMethod.Patch)
}
