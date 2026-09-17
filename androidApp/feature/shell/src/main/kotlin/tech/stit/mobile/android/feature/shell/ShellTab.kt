package tech.stit.mobile.android.feature.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import tech.stit.mobile.android.core.designsystem.StitIcons

// Корневые вкладки (shell.md#Root); в Destination их переводит app — feature не знает других feature.
enum class ShellTab(@DrawableRes val icon: Int, @StringRes val label: Int) {
    Todos(StitIcons.List, R.string.feature_shell_tab_todos),
    Profile(StitIcons.Person, R.string.feature_shell_tab_profile),
    Settings(StitIcons.Settings, R.string.feature_shell_tab_settings),
}
