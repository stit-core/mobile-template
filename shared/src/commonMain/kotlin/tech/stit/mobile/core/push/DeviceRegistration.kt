package tech.stit.mobile.core.push

import kotlinx.serialization.Serializable
import tech.stit.mobile.core.api.generated.DeviceRegisterInputBody

// Что уходит в PUT /api/v1/devices; сравнивается со снимком прошлой регистрации, чтобы не слать одно и то же каждый старт.
@Serializable
data class DeviceRegistration(
    val provider: PushProvider,
    val token: String,
    val platform: String,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
    val locale: String,
    val timeZone: String,
    val installationId: String,
    val sandbox: Boolean,
    val pushEnabled: Boolean,
)

internal fun DeviceRegistration.toBody(): DeviceRegisterInputBody = DeviceRegisterInputBody(
    appVersion = appVersion,
    locale = locale,
    platform = DeviceRegisterInputBody.Platform.entries.first { it.value == platform },
    provider = DeviceRegisterInputBody.Provider.entries.first { it.value == provider.apiName },
    token = token,
    deviceModel = deviceModel,
    installationId = installationId,
    osVersion = osVersion,
    pushEnabled = pushEnabled,
    sandbox = sandbox,
    timezone = timeZone,
)
