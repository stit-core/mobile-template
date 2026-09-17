package tech.stit.mobile.core.lib

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AttemptTest {
    @Test
    fun capturesFailuresButLetsCancellationThrough() {
        assertEquals(1, attempt { 1 }.getOrThrow())
        assertTrue(attempt<Int> { error("boom") }.isFailure)
        assertFailsWith<CancellationException> { attempt<Int> { throw CancellationException("stop") } }
    }
}
