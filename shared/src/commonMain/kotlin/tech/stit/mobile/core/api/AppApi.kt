package tech.stit.mobile.core.api

import io.ktor.client.request.parameter
import tech.stit.mobile.core.api.generated.AppRequirementsOutputBody

internal class AppApi(private val client: ApiClient) {
    // Публичный endpoint: работает до логина, поэтому без Authorization.
    suspend fun requirements(platform: String, version: String, build: Long, store: String): AppRequirementsOutputBody =
        client.get("/api/app/requirements") {
            skipAuth()
            parameter("platform", platform)
            parameter("version", version)
            parameter("build", build)
            parameter("store", store)
        }
}
