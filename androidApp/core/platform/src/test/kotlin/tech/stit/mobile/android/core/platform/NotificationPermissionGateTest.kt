package tech.stit.mobile.android.core.platform

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NotificationPermissionGateTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val gate = NotificationPermissionGate(application)

    @Test
    fun `выданное разрешение — granted, запрос сразу true`() = runTest {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(gate.isGranted())
        assertTrue(gate.request())
    }

    @Test
    fun `без разрешения и без Activity запрос невозможен`() = runTest {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(gate.isGranted())
        assertFalse(gate.request())
    }
}
