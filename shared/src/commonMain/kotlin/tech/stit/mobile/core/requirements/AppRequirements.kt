package tech.stit.mobile.core.requirements

import kotlinx.serialization.Serializable
import kotlin.time.Instant

// Runtime-конфиг с бэкенда (07 §4.3): статус версии, обслуживание, флаги-строки как flagkit, ссылки на правовые документы.
@Serializable
data class AppRequirements(
    val status: RequirementsStatus,
    val minVersion: String,
    val latestVersion: String,
    val storeUrl: String?,
    val maintenanceUntil: Instant?,
    val flags: Map<String, String>,
    val links: AppLinks,
) {
    fun flag(name: String, default: Boolean = false): Boolean = flags[name]?.toBooleanStrictOrNull() ?: default

    companion object {
        // Бэкенд без B-17 (404) или офлайн без кеша: приложение работает, ничего не блокируя.
        val Default = AppRequirements(
            status = RequirementsStatus.Ok,
            minVersion = "",
            latestVersion = "",
            storeUrl = null,
            maintenanceUntil = null,
            flags = emptyMap(),
            links = AppLinks(privacyPolicy = null, accountDeletion = null, support = null),
        )
    }
}

@Serializable
enum class RequirementsStatus {
    Ok,
    UpdateRecommended,
    UpdateRequired,
    Maintenance,
}

@Serializable
data class AppLinks(
    val privacyPolicy: String?,
    val accountDeletion: String?,
    val support: String?,
)
