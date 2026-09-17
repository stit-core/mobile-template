package tech.stit.mobile.core.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectivityStateTest {
    @Test
    fun startsWithInitialValueAndPublishesUpdates() {
        val state = ConnectivityState(initial = false)
        assertEquals(false, state.online.value)
        state.update(online = true)
        assertEquals(true, state.online.value)
        state.update(online = false)
        assertEquals(false, state.online.value)
    }
}
