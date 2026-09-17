package tech.stit.mobile.core.api

import io.ktor.http.HttpMethod
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetryPolicyTest {
    @Test
    fun retryAfterWinsAndIsCapped() {
        assertEquals(7_000, RetryPolicy.delayMs(1, "7"))
        assertEquals(300_000, RetryPolicy.delayMs(1, "9999"))
        assertEquals(0, RetryPolicy.delayMs(3, "0"))
    }

    @Test
    fun backoffGrowsExponentiallyWithJitter() {
        val first = RetryPolicy.delayMs(1, null, Random(1))
        val second = RetryPolicy.delayMs(2, null, Random(1))
        val garbage = RetryPolicy.delayMs(1, "soon", Random(1))
        assertTrue(first in 1_000..1_500, "first=$first")
        assertTrue(second in 2_000..2_500, "second=$second")
        assertEquals(first, garbage)
    }

    @Test
    fun onlyIdempotentOrKeyedRequestsRepeat() {
        val cases = listOf(
            Triple(HttpMethod.Get, false, true),
            Triple(HttpMethod.Delete, false, true),
            Triple(HttpMethod.Put, false, true),
            Triple(HttpMethod.Post, false, false),
            Triple(HttpMethod.Patch, false, false),
            Triple(HttpMethod.Post, true, true),
        )
        cases.forEach { (method, keyed, expected) ->
            assertEquals(expected, RetryPolicy.isRepeatable(method, keyed), "$method keyed=$keyed")
        }
    }
}
