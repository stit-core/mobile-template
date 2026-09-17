package tech.stit.mobile.android.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import tech.stit.mobile.PlatformDeps
import tech.stit.mobile.android.AppOptions
import tech.stit.mobile.android.core.platform.BiometricUnlock
import tech.stit.mobile.android.core.testing.FakeBiometricUnlock
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.core.testing.demoConfig
import tech.stit.mobile.core.config.AppConfig
import javax.inject.Singleton

// Тесты приложения идут в demo-конфигурации на фейках ОС: настоящий стек shared, MockTransport вместо сети.
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ConfigModule::class])
object TestConfigModule {
    @Provides
    @Singleton
    fun appConfig(): AppConfig = demoConfig()

    @Provides
    fun appOptions(): AppOptions = AppOptions(pushProviders = "", rustoreProjectId = "", biometricUnlock = false)
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PlatformModule::class])
object TestPlatformModule {
    @Provides
    @Singleton
    fun fakePlatformDeps(application: Application): FakePlatformDeps = FakePlatformDeps(filesDir = application.filesDir.absolutePath)

    @Provides
    fun platformDeps(fake: FakePlatformDeps): PlatformDeps = fake

    @Provides
    @Singleton
    fun biometricUnlock(): BiometricUnlock = FakeBiometricUnlock()
}
