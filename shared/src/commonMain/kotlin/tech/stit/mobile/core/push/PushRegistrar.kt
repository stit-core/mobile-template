package tech.stit.mobile.core.push

import kotlinx.serialization.Serializable
import tech.stit.mobile.core.api.ApiJson
import tech.stit.mobile.core.api.DevicesApi
import tech.stit.mobile.core.i18n.LocaleProvider
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.Platform
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.lib.currentPlatform
import tech.stit.mobile.core.lib.newUuid
import tech.stit.mobile.core.observability.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

// PUT /api/v1/devices по каждому транспорту (07 §6.1); повтор без изменений не чаще раза в сутки — снимок в Settings.
class PushRegistrar internal constructor(
    private val api: DevicesApi,
    private val transports: List<PushTransport>,
    private val permission: NotificationPermission,
    private val appInfo: AppInfo,
    private val locale: LocaleProvider,
    private val settings: Settings,
    private val sandbox: Boolean,
    private val clock: Clock,
    private val logger: Logger,
) {
    @Serializable
    private class Registered(val registration: DeviceRegistration, val deviceId: String, val at: Long)

    // Возвращает число отправленных регистраций; ошибки сети не роняют старт — следующая попытка на следующем старте.
    suspend fun register(force: Boolean = false): Int {
        val pushEnabled = permission.isGranted()
        val sent = transports.count { transport -> registerOne(transport, pushEnabled, force) }
        logger.info("push: зарегистрировано $sent из ${transports.size}, pushEnabled=$pushEnabled")
        return sent
    }

    // Logout: устройство отвязывается, пока access ещё валиден; ошибка API не блокирует выход.
    suspend fun unregister() {
        for (transport in transports) {
            val registered = load(transport.provider) ?: continue
            runCatching {
                api.unregister(registered.deviceId)
            }.onFailure { logger.warn("push: не удалось отвязать ${transport.provider}", it) }
            settings.remove(key(transport.provider))
        }
    }

    // Пред-запрос разрешения (07 §6, п. 5): системный диалог → регистрация; отказ запоминается, чтобы не спрашивать снова.
    suspend fun optIn(): Boolean {
        val granted = permission.isGranted() || permission.request()
        settings.putString(SettingsKeys.PUSH_DECLINED, (!granted).toString())
        if (granted) register(force = true)
        return granted
    }

    suspend fun optOut() {
        settings.putString(SettingsKeys.PUSH_DECLINED, "true")
        unregister()
    }

    suspend fun status(): PushStatus = when {
        permission.isGranted() && settings.getString(SettingsKeys.PUSH_DECLINED) != "true" -> PushStatus.Granted
        settings.getString(SettingsKeys.PUSH_DECLINED) == "true" -> PushStatus.Declined
        else -> PushStatus.NotAsked
    }

    suspend fun installationId(): String = settings.getString(SettingsKeys.INSTALLATION_ID)
        ?: newUuid().also { settings.putString(SettingsKeys.INSTALLATION_ID, it) }

    private suspend fun registerOne(transport: PushTransport, pushEnabled: Boolean, force: Boolean): Boolean {
        val registration = transport.token()?.let { registration(transport.provider, it, pushEnabled) }
        val needed = registration != null && (force || !load(transport.provider).isFresh(registration))
        if (needed) {
            val device = api.register(registration!!.toBody()).data
            save(transport.provider, Registered(registration, device.id, clock.now().epochSeconds))
        }
        return needed
    }

    private fun Registered?.isFresh(registration: DeviceRegistration): Boolean =
        this != null && this.registration == registration && clock.now().epochSeconds - at < REPEAT_AFTER_SECONDS

    private suspend fun registration(provider: PushProvider, token: String, pushEnabled: Boolean) = DeviceRegistration(
        provider = provider,
        token = token,
        platform = if (currentPlatform == Platform.Ios) "ios" else "android",
        appVersion = appInfo.versionWithBuild,
        osVersion = appInfo.osVersion,
        deviceModel = appInfo.deviceModel,
        locale = locale.languageTag,
        timeZone = locale.timeZone,
        installationId = installationId(),
        sandbox = sandbox,
        pushEnabled = pushEnabled,
    )

    private suspend fun load(provider: PushProvider): Registered? =
        settings.getString(key(provider))?.let { runCatching { ApiJson.decodeFromString(Registered.serializer(), it) }.getOrNull() }

    private suspend fun save(provider: PushProvider, registered: Registered) =
        settings.putString(key(provider), ApiJson.encodeToString(Registered.serializer(), registered))

    private fun key(provider: PushProvider) = SettingsKeys.PUSH_REGISTRATION_PREFIX + provider.apiName

    private companion object {
        val REPEAT_AFTER_SECONDS = 24.hours.inWholeSeconds
    }
}
