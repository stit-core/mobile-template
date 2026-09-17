package tech.stit.mobile.core.api

import io.ktor.client.request.setBody
import tech.stit.mobile.core.api.generated.UserMeOutputBody
import tech.stit.mobile.core.api.generated.UserUpdateInputBody
import tech.stit.mobile.core.api.generated.UserUpdateOutputBody

internal class UsersApi(private val client: ApiClient) {
    suspend fun me(): UserMeOutputBody = client.get("/api/users/me")

    suspend fun updateName(id: String, name: String, version: Long): UserUpdateOutputBody = client.patch("/api/users/$id") {
        setBody(UserUpdateInputBody(version = version, name = name))
    }

    suspend fun delete(id: String) = client.delete("/api/users/$id")
}
