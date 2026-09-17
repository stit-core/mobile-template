package tech.stit.mobile.core.observability

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LogcatSinkTest {
    @Test
    fun forwardsEveryLevelToLogcat() {
        val logger = Logger("Test", LogcatSink())
        logger.debug("d")
        logger.info("i")
        logger.warn("w")
        logger.error("e", IllegalStateException("boom"))
        val lines = ShadowLog.getLogsForTag("Test").map { it.msg }
        assertEquals(listOf("d", "i", "w", "e"), lines)
    }
}
