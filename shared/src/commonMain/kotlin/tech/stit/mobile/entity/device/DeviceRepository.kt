package tech.stit.mobile.entity.device

import tech.stit.mobile.core.api.DevicesApi

// «Мои устройства» для экрана сессий; регистрацию этого устройства ведёт core/push/PushRegistrar.
class DeviceRepository internal constructor(private val api: DevicesApi) {
    suspend fun list(): List<Device> = api.list().data.map { it.toModel() }

    suspend fun unregister(id: String) = api.unregister(id)
}
