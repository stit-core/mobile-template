package tech.stit.mobile.core.api

import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import tech.stit.mobile.core.api.generated.ChangePasswordInputBody
import tech.stit.mobile.core.api.generated.LoginInputBody
import tech.stit.mobile.core.api.generated.LoginOutputBody
import tech.stit.mobile.core.api.generated.RefreshInputBody
import tech.stit.mobile.core.api.generated.SessionListOutputBody
import tech.stit.mobile.core.api.generated.TokenPairBody

internal class AuthApi(private val client: ApiClient) {
    suspend fun login(email: String, password: String): LoginOutputBody = client.post("/api/auth/login") {
        skipAuth()
        setBody(LoginInputBody(email = email, password = password))
    }

    // Bearer: refresh в теле → 200 TokenPairBody; cookie-режим: без тела → 204 и новые cookie.
    suspend fun refresh(refreshToken: String?): TokenPairBody? {
        val response = client.execute(HttpMethod.Post, "/api/auth/refresh") {
            skipAuth()
            noRetry()
            refreshToken?.let { setBody(RefreshInputBody(refreshToken = it)) }
        }
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    suspend fun logout(refreshToken: String?) {
        client.execute(HttpMethod.Post, "/api/auth/logout") {
            noRetry()
            refreshToken?.let { setBody(RefreshInputBody(refreshToken = it)) }
        }
    }

    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String) {
        client.execute(HttpMethod.Post, "/api/auth/change-password") {
            setBody(ChangePasswordInputBody(userId = userId, oldPassword = oldPassword, newPassword = newPassword))
        }
    }

    suspend fun sessions(): SessionListOutputBody = client.get("/api/auth/sessions")

    suspend fun revokeSession(jti: String) = client.delete("/api/auth/sessions/$jti")

    suspend fun revokeAllSessions() = client.delete("/api/auth/sessions")
}
