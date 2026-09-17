package tech.stit.mobile.core.config

// API_MODE: mock — MockTransport и фикстуры mock/ вместо сети (flavor demo, App Review); real — Ktor-движок платформы.
enum class ApiMode {
    Real,
    Mock,
    ;

    companion object {
        fun parse(raw: String): ApiMode = parseEnum(raw, entries, "ый режим API")
    }
}
