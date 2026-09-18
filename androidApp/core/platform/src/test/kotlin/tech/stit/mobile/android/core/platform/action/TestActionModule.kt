package tech.stit.mobile.android.core.platform.action

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.core.testing.demoConfig
import javax.inject.Singleton

// В приложении контейнер даёт app, а перерисовку виджета — модуль виджетов; в тесте модуля обе половины подставные.
@Module
@InstallIn(SingletonComponent::class)
internal object TestActionModule {
    @Provides
    @Singleton
    fun sharedContainer(application: Application): SharedContainer =
        SharedContainer(FakePlatformDeps(application.filesDir.absolutePath), demoConfig())

    @Provides
    @Singleton
    fun widgetRefresher(): RecordingWidgetRefresher = RecordingWidgetRefresher()

    @Provides
    fun widgetRefresherPort(recording: RecordingWidgetRefresher): WidgetRefresher = recording
}

internal class RecordingWidgetRefresher : WidgetRefresher {
    val slugs = mutableListOf<String>()

    override suspend fun refresh(slug: String) {
        slugs += slug
    }
}
