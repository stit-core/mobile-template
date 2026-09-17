package tech.stit.mobile.core.api

import kotlinx.serialization.Serializable
import tech.stit.mobile.core.i18n.MessageKey

// Ошибки транспорта, с которыми работают репозитории и презентеры; всё остальное — баги, они не ловятся.
sealed class ApiError(message: String) : Exception(message) {
    // application/problem+json бэкенда: code — единственный ключ для ветвления и локализации (07 §2.6).
    class Http(
        val status: Int,
        val code: String,
        val detail: String?,
        val requestId: String?,
        val fieldErrors: Map<String, String>,
    ) : ApiError("$status $code")

    // Нет сети, таймаут, обрыв — запрос можно повторить позже, сессия не трогается.
    class Network(cause: Throwable) : ApiError(cause.message ?: "network")

    val messageKey: MessageKey
        get() = when (this) {
            is Http -> if (fieldErrors.isEmpty()) MessageKey.forErrorCode(code) else MessageKey.ValidationError
            is Network -> MessageKey.NetworkError
        }
}

fun Throwable.toMessageKey(): MessageKey = (this as? ApiError)?.messageKey ?: MessageKey.UnknownError

// Разбор руками: `code` — контракт httpkit, errors[].location вида body.email → поле формы (07 T-6).
@Serializable
internal class ProblemBody(
    val code: String? = null,
    val detail: String? = null,
    val errors: List<ProblemField>? = null,
) {
    fun fieldErrors(): Map<String, String> = errors.orEmpty()
        .filter { it.location != null && it.message != null }
        .associate { it.location!!.substringAfterLast('.') to it.message!! }
}

@Serializable
internal class ProblemField(val location: String? = null, val message: String? = null)
