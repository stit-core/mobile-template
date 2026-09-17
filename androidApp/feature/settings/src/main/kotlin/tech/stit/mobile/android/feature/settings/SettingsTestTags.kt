package tech.stit.mobile.android.feature.settings

object SettingsTestTags {
    const val LIST = "settings_list"
    const val DEMO_LATENCY = "settings_demo_latency"
    const val PUSH_ACCEPT = "push_accept"
    const val PUSH_DECLINE = "push_decline"

    fun theme(name: String): String = "settings_theme_${name.lowercase()}"

    fun language(name: String): String = "settings_language_${name.lowercase()}"

    fun scenario(name: String): String = "settings_scenario_${name.lowercase()}"
}
