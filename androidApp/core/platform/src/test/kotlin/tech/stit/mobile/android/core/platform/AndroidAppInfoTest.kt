package tech.stit.mobile.android.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import tech.stit.mobile.core.lib.Store
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidAppInfoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `магазин выводится из установщика`() {
        val cases = mapOf(
            "com.android.vending" to Store.Play,
            "ru.vk.store" to Store.RuStore,
            "com.huawei.appmarket" to Store.AppGallery,
            "org.fdroid.fdroid" to Store.Direct,
            null to Store.Direct,
        )
        cases.forEach { (installer, store) ->
            shadowOf(context.packageManager).setInstallSourceInfo(context.packageName, null, installer)
            assertEquals(store, context.installedStore(), installer.toString())
        }
    }

    @Test
    fun `AppInfo собирается из пакета и ОС`() {
        val info = context.androidAppInfo()
        assertEquals(context.packageName, info.appId)
        assertTrue(info.osVersion.startsWith("Android "), info.osVersion)
        assertTrue(info.deviceModel.isNotBlank())
        assertTrue(AndroidLocaleProvider.languageTag.isNotBlank())
        assertTrue(AndroidLocaleProvider.timeZone.isNotBlank())
    }
}
