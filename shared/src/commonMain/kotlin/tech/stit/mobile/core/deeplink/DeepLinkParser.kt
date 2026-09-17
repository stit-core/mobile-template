package tech.stit.mobile.core.deeplink

import io.ktor.http.Url

// App Links https://<host>/… и custom scheme <scheme>://… (07 §5.3); чужой хост или неизвестный путь → null.
class DeepLinkParser(private val host: String, private val scheme: String) {
    fun parse(url: String): Route? {
        val parsed = runCatching { Url(url) }.getOrNull()
        val segments = when {
            parsed == null -> null
            parsed.protocol.name == "https" && parsed.host.equals(host, ignoreCase = true) -> parsed.pathSegments()
            parsed.protocol.name == scheme -> listOf(parsed.host) + parsed.pathSegments()
            else -> null
        }
        return segments?.let { route(it.filter(String::isNotEmpty)) }
    }

    private fun Url.pathSegments(): List<String> = encodedPath.split('/')

    private fun route(segments: List<String>): Route? = when {
        segments.isEmpty() -> Route.Home

        segments.size == 1 -> when (segments[0]) {
            "login" -> Route.Login
            "profile" -> Route.Profile
            "settings" -> Route.Settings
            "todos" -> Route.TodoList
            else -> null
        }

        segments.size == 2 && segments[0] == "todos" -> Route.TodoDetail(segments[1])

        else -> null
    }
}
