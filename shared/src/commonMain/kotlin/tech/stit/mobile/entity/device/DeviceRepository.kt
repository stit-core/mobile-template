package tech.stit.mobile.entity.device

import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.DevicesApi
import kotlin.coroutines.cancellation.CancellationException

// «Мои устройства» для экрана сессий; регистрацию этого устройства ведёт core/push/PushRegistrar.
class DeviceRepository internal constructor(private val api: DevicesApi) {
    @Throws(ApiError::class, CancellationException::class)
    suspend fun list(): List<Device> = api.list().data.map { it.toModel() }

    @Throws(ApiError::class, CancellationException::class)
    suspend fun unregister(id: String) = api.unregister(id)
}
