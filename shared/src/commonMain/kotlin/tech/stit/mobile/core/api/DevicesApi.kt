package tech.stit.mobile.core.api

import io.ktor.client.request.setBody
import tech.stit.mobile.core.api.generated.DeviceListOutputBody
import tech.stit.mobile.core.api.generated.DeviceOutputBody
import tech.stit.mobile.core.api.generated.DeviceRegisterInputBody

internal class DevicesApi(private val client: ApiClient) {
    suspend fun register(body: DeviceRegisterInputBody): DeviceOutputBody = client.put("/api/v1/devices") {
        setBody(body)
    }

    suspend fun list(): DeviceListOutputBody = client.get("/api/v1/devices")

    suspend fun unregister(id: String) = client.delete("/api/v1/devices/$id")
}
