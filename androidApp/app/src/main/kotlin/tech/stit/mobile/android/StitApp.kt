package tech.stit.mobile.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.feature.settings.SettingsViewModel
import tech.stit.mobile.feature.settings.ThemeMode

// Корень приложения: тема — из SettingsPresenter (один экземпляр на Activity), дальше — оболочка и навигация.
@Composable
fun StitApp(modifier: Modifier = Modifier, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    StitTheme(darkTheme = settings.theme.isDark()) {
        // testTag → resource-id: Maestro-флоу e2e/android адресуют элементы теми же тегами, что Compose-тесты.
        AppNavigation(modifier = modifier.semantics { testTagsAsResourceId = true })
    }
}

@Composable
private fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
