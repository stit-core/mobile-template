package tech.stit.mobile.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.core.config.AppConfig
import javax.inject.Singleton

// Composition root shared — один на процесс; ViewModel'и берут из него фабрики презентеров.
@Module
@InstallIn(SingletonComponent::class)
object SharedModule {
    @Provides
    @Singleton
    fun sharedContainer(platform: PlatformDeps, config: AppConfig): SharedContainer = SharedContainer(platform, config)
}
