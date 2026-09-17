package tech.stit.mobile.core.requirements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.api.AppApi
import tech.stit.mobile.core.api.generated.AppRequirementsView
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.FileCache
import tech.stit.mobile.core.lib.Platform
import tech.stit.mobile.core.lib.currentPlatform
import tech.stit.mobile.core.observability.Logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// Не чаще раза в 5 минут (07 §4.3); 404 (бэкенд без B-17) и офлайн без кеша → Default, приложение не блокируется.
class RequirementsClient internal constructor(
    private val api: AppApi,
    private val cache: FileCache<AppRequirements>,
    private val appInfo: AppInfo,
    private val clock: Clock,
    private val logger: Logger,
) {
    private val _current = MutableStateFlow(AppRequirements.Default)
    val current: StateFlow<AppRequirements> = _current.asStateFlow()
    private var fetchedAt: Instant? = null

    suspend fun fetch(force: Boolean = false): AppRequirements {
        val last = fetchedAt
        if (!force && last != null && clock.now() - last < MIN_INTERVAL) return _current.value
        val fresh = try {
            api.requirements(platformName(), appInfo.version, appInfo.build, appInfo.store.apiName).data.toModel().also { cache.write(it) }
        } catch (e: ApiError.Http) {
            if (e.status == NOT_FOUND) AppRequirements.Default else fallback(e)
        } catch (e: ApiError.Network) {
            fallback(e)
        }
        fetchedAt = clock.now()
        _current.value = fresh
        return fresh
    }

    suspend fun restore() {
        cache.read()?.let { _current.value = it }
    }

    private suspend fun fallback(e: ApiError): AppRequirements {
        logger.warn("requirements недоступны, берём кеш", e)
        return cache.read() ?: AppRequirements.Default
    }

    private fun platformName(): String = if (currentPlatform == Platform.Ios) "ios" else "android"

    private companion object {
        val MIN_INTERVAL = 5.minutes
        const val NOT_FOUND = 404
    }
}

internal fun AppRequirementsView.toModel(): AppRequirements = AppRequirements(
    status = when (status) {
        AppRequirementsView.Status.Ok -> RequirementsStatus.Ok
        AppRequirementsView.Status.UpdateRecommended -> RequirementsStatus.UpdateRecommended
        AppRequirementsView.Status.UpdateRequired -> RequirementsStatus.UpdateRequired
        AppRequirementsView.Status.Maintenance -> RequirementsStatus.Maintenance
    },
    minVersion = minVersion,
    latestVersion = latestVersion,
    storeUrl = storeUrl,
    maintenanceUntil = maintenanceUntil,
    flags = flags,
    links = AppLinks(privacyPolicy = links.privacyPolicy, accountDeletion = links.accountDeletion, support = links.support),
)
