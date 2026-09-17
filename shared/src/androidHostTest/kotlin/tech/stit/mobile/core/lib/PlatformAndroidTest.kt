package tech.stit.mobile.core.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformAndroidTest {
    @Test
    fun androidTargetReportsAndroid() {
        assertEquals(Platform.Android, currentPlatform)
    }
}
