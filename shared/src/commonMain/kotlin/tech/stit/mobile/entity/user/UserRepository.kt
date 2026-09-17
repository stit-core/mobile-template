package tech.stit.mobile.entity.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.AuthApi
import tech.stit.mobile.core.api.UsersApi
import tech.stit.mobile.core.api.generated.UserView
import tech.stit.mobile.core.lib.FileCache
import tech.stit.mobile.core.observability.TelemetrySink
import kotlin.coroutines.cancellation.CancellationException

// SSOT текущего пользователя: in-memory StateFlow + снимок в FileCache для офлайн-старта.
class UserRepository internal constructor(
    private val api: UsersApi,
    private val authApi: AuthApi,
    private val cache: FileCache<UserView>,
    private val telemetry: TelemetrySink,
) {
    private val _me = MutableStateFlow<User?>(null)
    val me: StateFlow<User?> = _me.asStateFlow()

    suspend fun restore() {
        cache.read()?.let(::publish)
    }

    @Throws(ApiError::class, CancellationException::class)
    suspend fun refresh(): User = publish(api.me().data.also { cache.write(it) })

    // Оптимистично: имя показывается сразу, откат — если бэкенд отказал.
    @Throws(ApiError::class, CancellationException::class)
    suspend fun updateName(name: String): User {
        val current = checkNotNull(_me.value) { "нет текущего пользователя" }
        _me.value = current.copy(name = name)
        val view = try {
            api.updateName(current.id, name, current.version).data
        } catch (e: ApiError) {
            _me.value = current
            throw e
        }
        cache.write(view)
        return publish(view)
    }

    @Throws(ApiError::class, CancellationException::class)
    suspend fun changePassword(oldPassword: String, newPassword: String) {
        val current = checkNotNull(_me.value) { "нет текущего пользователя" }
        authApi.changePassword(current.id, oldPassword, newPassword)
    }

    // App Store 5.1.1(v): удаление аккаунта из приложения; сессию после этого закрывает SessionRepository.logout().
    @Throws(ApiError::class, CancellationException::class)
    suspend fun deleteAccount() {
        val current = checkNotNull(_me.value) { "нет текущего пользователя" }
        api.delete(current.id)
    }

    suspend fun clear() {
        _me.value = null
        cache.clear()
        telemetry.setUserId(null)
    }

    private fun publish(view: UserView): User = view.toModel().also {
        _me.value = it
        telemetry.setUserId(it.id)
    }
}
