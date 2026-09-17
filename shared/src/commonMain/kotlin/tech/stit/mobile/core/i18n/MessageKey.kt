package tech.stit.mobile.core.i18n

// Текст живёт в strings.xml / Localizable.xcstrings, shared отдаёт ключ; ошибки API — error.<code> (07 §2.6).
data class MessageKey(val value: String) {
    companion object {
        val NetworkError = MessageKey("error.network")
        val UnknownError = MessageKey("error.unknown")
        val ValidationError = MessageKey("error.validation")

        fun forErrorCode(code: String): MessageKey = MessageKey("error.${code.lowercase()}")
    }
}

// Коды ошибок, которые shared ветвит или показывает: i18n-check сверяет их с ресурсами платформ.
object KnownErrorCodes {
    const val AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS"
    const val AUTH_REFRESH_EXPIRED = "AUTH_REFRESH_EXPIRED"
    const val AUTH_REFRESH_REUSED = "AUTH_REFRESH_REUSED"
    const val AUTH_ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val INTERNAL = "INTERNAL"

    val all: List<String> = listOf(
        AUTH_INVALID_CREDENTIALS,
        AUTH_REFRESH_EXPIRED,
        AUTH_REFRESH_REUSED,
        AUTH_ACCOUNT_LOCKED,
        RATE_LIMITED,
        INTERNAL,
    )
}
