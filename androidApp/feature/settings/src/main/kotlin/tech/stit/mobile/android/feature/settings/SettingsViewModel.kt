package tech.stit.mobile.android.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.settings.SettingsIntent
import tech.stit.mobile.feature.settings.SettingsState
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.settingsPresenter()
    val state: StateFlow<SettingsState> = presenter.state

    init {
        presenter.dispatch(SettingsIntent.Start)
    }

    // Язык применяет платформа сразу (per-app locale, AppCompat хранит его сам); презентер держит выбор для экрана.
    fun dispatch(intent: SettingsIntent) {
        presenter.dispatch(intent)
        if (intent is SettingsIntent.SetLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(intent.language.tag.orEmpty()))
        }
    }

    override fun onCleared() = presenter.close()
}
