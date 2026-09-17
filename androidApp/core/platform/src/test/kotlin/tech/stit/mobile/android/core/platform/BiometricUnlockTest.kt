package tech.stit.mobile.android.core.platform

import android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BiometricUnlockTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager: ShadowBiometricManager = Shadow.extract(context.getSystemService(BiometricManager::class.java))

    @Test
    fun `замок нужен только при включённой опции и доступной биометрии`() {
        manager.setCanAuthenticate(true)
        assertTrue(DefaultBiometricUnlock(context, enabled = true).required)
        assertFalse(DefaultBiometricUnlock(context, enabled = false).required)

        manager.setCanAuthenticate(false)
        assertFalse(DefaultBiometricUnlock(context, enabled = true).required)
    }
}
