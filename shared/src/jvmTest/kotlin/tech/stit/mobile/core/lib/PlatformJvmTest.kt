package tech.stit.mobile.core.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformJvmTest {
    @Test
    fun jvmTargetReportsJvm() {
        assertEquals(Platform.Jvm, currentPlatform)
    }
}
