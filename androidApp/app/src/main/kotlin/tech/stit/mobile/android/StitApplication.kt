package tech.stit.mobile.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.SentryCrashReporter
import tech.stit.mobile.android.core.platform.push.NotificationChannels
import tech.stit.mobile.android.widget.WidgetUpdates
import tech.stit.mobile.core.config.AppConfig
import javax.inject.Inject

@HiltAndroidApp
class StitApplication : Application() {
    @Inject
    lateinit var config: AppConfig

    @Inject
    lateinit var platform: PlatformDeps

    @Inject
    lateinit var container: SharedContainer

    @Inject
    lateinit var widgetUpdates: WidgetUpdates

    // Scope процесса: коллектор снимка виджета не привязан ни к экрану, ни к задаче планировщика.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        SentryCrashReporter.init(this, config, platform.appInfo)
        // Периодические задачи подтверждаются на каждом старте: уникальная работа с политикой KEEP уже запланированную не сбрасывает.
        container.backgroundTasks.forEach(platform.background::schedule)
        widgetUpdates.start(appScope)
    }
}
