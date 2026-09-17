package tech.stit.mobile.android.core.platform.push

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import tech.stit.mobile.core.push.PushProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PushTransportsTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `PUSH_PROVIDERS задаёт транспорты и их порядок`() {
        val transports = pushTransports(application, "rustore, fcm", rustoreProjectId = "")
        assertEquals(listOf(PushProvider.Rustore, PushProvider.Fcm), transports.map { it.provider })
        assertTrue(pushTransports(application, "", rustoreProjectId = "").isEmpty())
        assertFailsWith<IllegalStateException> { pushTransports(application, "hms", rustoreProjectId = "") }
    }

    @Test
    fun `без Firebase-конфига и без проекта RuStore токенов нет`() = runTest {
        assertNull(FcmTransport(application).token())
        assertNull(RuStoreTransport(application, projectId = "").token())
    }
}
