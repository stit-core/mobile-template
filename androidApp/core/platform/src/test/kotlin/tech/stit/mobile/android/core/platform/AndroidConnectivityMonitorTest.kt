package tech.stit.mobile.android.core.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidConnectivityMonitorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    @Test
    fun `онлайн следует за проверенной сетью и её потерей`() {
        val network = checkNotNull(manager.activeNetwork)
        shadowOf(manager).setNetworkCapabilities(network, validated())
        val monitor = AndroidConnectivityMonitor(context)
        assertTrue(monitor.online.value)

        shadowOf(manager).networkCallbacks.forEach { it.onLost(network) }
        assertFalse(monitor.online.value)

        shadowOf(manager).networkCallbacks.forEach { it.onCapabilitiesChanged(network, validated()) }
        assertTrue(monitor.online.value)
    }

    @Test
    fun `сеть без интернета — офлайн`() {
        val network = checkNotNull(manager.activeNetwork)
        shadowOf(manager).setNetworkCapabilities(network, ShadowNetworkCapabilities.newInstance())
        assertFalse(AndroidConnectivityMonitor(context).online.value)
    }

    private fun validated(): NetworkCapabilities = ShadowNetworkCapabilities.newInstance().also {
        shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
