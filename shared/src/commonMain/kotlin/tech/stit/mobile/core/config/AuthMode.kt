package tech.stit.mobile.core.config

// AUTH_MODE: bearer — канон (07 §1.3b); cookie — совместимость с бэкендом без B-1…B-3, сессия только в памяти.
enum class AuthMode {
    Bearer,
    Cookie,
    ;

    companion object {
        fun parse(raw: String): AuthMode = parseEnum(raw, entries, "ый режим auth")
    }
}
