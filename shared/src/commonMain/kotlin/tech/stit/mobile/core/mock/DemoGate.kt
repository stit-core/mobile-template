package tech.stit.mobile.core.mock

import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import tech.stit.mobile.core.api.ApiClient
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.core.requirements.RequirementsClient

// Runtime-переключатель demo в обычной сборке (05 §1.3): ревьюер магазина проходит приложение на фикстурах без
// отдельной сборки. Вход открывает только флаг requirements.demo_login; сама demo-сессия идёт в MockTransport и в
// собственные хранилища, поэтому реальные токены, настройки и кеши она не видит и не переписывает. Demo не
// переживает перезапуск процесса: холодный старт — всегда обычный режим.
internal class DemoGate(
    private val requirements: RequirementsClient,
    private val sessionStore: SessionStore,
    private val storage: DemoStorage,
    private val apiClient: ApiClient,
    private val newTransport: () -> MockTransport,
    private val realEngine: () -> HttpClientEngine,
    private val afterSwitch: suspend () -> Unit,
    private val logger: Logger,
) {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    // Requirements недоступны (404, офлайн без кеша) — Default без флагов, и вход в demo не показывается.
    val available: Flow<Boolean> = requirements.current.map { it.flag(FLAG) }

    private var transport: MockTransport? = null

    suspend fun enter() {
        if (_active.value || !requirements.current.value.flag(FLAG)) return
        val mock = transport ?: newTransport().also { transport = it }
        // Метка пишется до подмены хранилищ, то есть в реальные настройки: по ней первый старт после убитого
        // в demo процесса вычистит снимки фикстур с диска.
        storage.settings.putString(SettingsKeys.DEMO_ACTIVE, true.toString())
        storage.enter()
        apiClient.useEngine(mock.engine)
        _active.value = true
        sessionStore.reset()
        // Сценарий с посеянной сессией: ревьюер попадает внутрь приложения, а не на второй экран входа.
        DemoMode(mock, storage.settings, storage.secureStore, sessionStore).apply(MockScenario.Authenticated)
        switched()
        logger.info("demo-режим включён по флагу $FLAG")
    }

    suspend fun leave() {
        if (!_active.value) return
        _active.value = false
        apiClient.useEngine(realEngine())
        storage.leave()
        storage.settings.remove(SettingsKeys.DEMO_ACTIVE)
        sessionStore.reset()
        switched()
        logger.info("demo-режим выключен")
    }

    // Старт приложения: процесс могли убить посреди demo, и на диске остались снимки фикстур — чистим их прежде,
    // чем обычный режим успеет показать их как свои.
    suspend fun restore() {
        if (storage.settings.getString(SettingsKeys.DEMO_ACTIVE) != true.toString()) return
        storage.settings.remove(SettingsKeys.DEMO_ACTIVE)
        sessionStore.reset()
        switched()
        logger.info("следы demo-режима очищены после перезапуска")
    }

    // Переключение обязано догнать и то, что живёт мимо сессии: снимок requirements (флаги и правовые ссылки
    // прежнего режима) и снимок виджета за пределами процесса приложения.
    private suspend fun switched() {
        requirements.reset()
        attempt { requirements.fetch(force = true) }
        afterSwitch()
    }

    private companion object {
        const val FLAG = "demo_login"
    }
}
