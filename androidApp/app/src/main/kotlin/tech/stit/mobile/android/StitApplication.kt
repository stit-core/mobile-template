package tech.stit.mobile.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.android.core.platform.SentryCrashReporter
import tech.stit.mobile.android.core.platform.push.NotificationChannels
import tech.stit.mobile.core.config.AppConfig
import javax.inject.Inject

@HiltAndroidApp
class StitApplication : Application() {
    @Inject
    lateinit var config: AppConfig

    @Inject
    lateinit var platform: PlatformDeps

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        SentryCrashReporter.init(this, config, platform.appInfo)
    }
}
