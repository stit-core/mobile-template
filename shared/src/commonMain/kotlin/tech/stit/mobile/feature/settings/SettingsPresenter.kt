package tech.stit.mobile.feature.settings

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.stit.mobile.core.config.AppConfig
import tech.stit.mobile.core.lib.AppInfo
import tech.stit.mobile.core.lib.DispatcherProvider
import tech.stit.mobile.core.lib.Settings
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.lib.attempt
import tech.stit.mobile.core.lib.presenterScope
import tech.stit.mobile.core.push.PushRegistrar
import tech.stit.mobile.core.push.PushStatus
import tech.stit.mobile.core.requirements.RequirementsClient

// Тема, язык, уведомления, «О приложении» (версия/build/APP_ENV) и правовые ссылки; выбор хранится в Settings.
class SettingsPresenter internal constructor(
    private val settings: Settings,
    private val pushRegistrar: PushRegistrar,
    private val requirements: RequirementsClient,
    private val appInfo: AppInfo,
    private val config: AppConfig,
    dispatchers: DispatcherProvider,
) {
    private val scope = presenterScope(dispatchers)
    private val _state = MutableStateFlow(SettingsState(version = appInfo.version, build = appInfo.build, env = config.env))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun dispatch(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Start -> start()

            is SettingsIntent.SetTheme -> scope.launch {
                settings.putString(SettingsKeys.THEME, intent.theme.name)
                _state.update { it.copy(theme = intent.theme) }
            }

            is SettingsIntent.SetLanguage -> scope.launch {
                settings.putString(SettingsKeys.LANGUAGE, intent.language.name)
                _state.update { it.copy(language = intent.language) }
            }

            is SettingsIntent.SetNotifications -> scope.launch {
                val enabled = if (intent.enabled) {
                    attempt {
                        pushRegistrar.optIn()
                    }.getOrDefault(false)
                } else {
                    false.also { pushRegistrar.optOut() }
                }
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }
    }

    fun close() = scope.cancel()

    private fun start() {
        scope.launch { requirements.current.collect { req -> _state.update { it.copy(links = req.links) } } }
        scope.launch {
            val theme = settings.getString(SettingsKeys.THEME)?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            val language = settings.getString(SettingsKeys.LANGUAGE)?.let { name -> AppLanguage.entries.firstOrNull { it.name == name } }
            val push = pushRegistrar.status() == PushStatus.Granted
            _state.update { it.copy(theme = theme ?: it.theme, language = language ?: it.language, notificationsEnabled = push) }
        }
    }
}
