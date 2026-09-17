package tech.stit.mobile.feature.shell

sealed class ShellIntent {
    data object Start : ShellIntent()

    data class OpenLink(val url: String) : ShellIntent()

    data object RouteConsumed : ShellIntent()
}
