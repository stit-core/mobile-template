package tech.stit.mobile.android.core.ui

import org.junit.Test
import tech.stit.mobile.android.core.testing.ScreenshotTest
import kotlin.time.Instant

class StatesScreenshotTest : ScreenshotTest() {
    @Test
    fun `загрузка`() = capture("state_loading") { LoadingState() }

    @Test
    fun `пусто с действием`() = capture("state_empty") {
        EmptyState(title = "Пока пусто", description = "Добавьте первую задачу", actionLabel = "Добавить")
    }

    @Test
    fun `ошибка с повтором`() = capture("state_error") { ErrorState(message = "Ошибка сервера. Попробуйте позже.", onRetry = {}) }

    @Test
    fun `ошибка в тёмной теме`() = capture("state_error_dark", darkTheme = true) {
        ErrorState(message = "Ошибка сервера. Попробуйте позже.", onRetry = {})
    }

    @Test
    fun `баннер офлайн`() = capture("state_offline_banner") { OfflineBanner() }

    @Test
    fun `нужно обновление`() = capture("state_update_required") { UpdateRequired(storeUrl = "https://example.com", onOpenStore = {}) }

    @Test
    fun `технические работы`() = capture("state_maintenance") {
        Maintenance(until = Instant.parse("2026-09-17T15:00:00Z"), onRetry = {})
    }

    @Test
    fun `рекомендация обновиться`() = capture("state_update_recommended") {
        UpdateRecommendedBanner(latestVersion = "1.2.0", onDismiss = {})
    }

    @Test
    fun `заблокировано биометрией`() = capture("state_locked") { LockedState(onUnlock = {}) }
}
