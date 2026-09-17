package tech.stit.mobile.android.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import tech.stit.mobile.android.core.designsystem.StitTheme
import tech.stit.mobile.android.core.designsystem.component.StitListItem
import tech.stit.mobile.android.core.designsystem.component.StitTextField
import tech.stit.mobile.android.core.designsystem.component.StitTopBar
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.feature.demo.DemoPanelIntent
import tech.stit.mobile.feature.demo.DemoPanelState
import tech.stit.mobile.feature.settings.AppLanguage
import tech.stit.mobile.feature.settings.SettingsIntent
import tech.stit.mobile.feature.settings.SettingsState
import tech.stit.mobile.feature.settings.ThemeMode

@Composable
fun SettingsScreen(
    state: SettingsState,
    demo: DemoPanelState,
    onIntent: (SettingsIntent) -> Unit,
    onDemoIntent: (DemoPanelIntent) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { StitTopBar(title = stringResource(R.string.feature_settings_title)) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().testTag(SettingsTestTags.LIST)) {
            section(R.string.feature_settings_theme)
            items(ThemeMode.entries) { mode ->
                ChoiceRow(
                    label = stringResource(mode.label()),
                    selected = state.theme == mode,
                    onClick = { onIntent(SettingsIntent.SetTheme(mode)) },
                    modifier = Modifier.testTag(SettingsTestTags.theme(mode.name)),
                )
            }
            section(R.string.feature_settings_language)
            items(AppLanguage.entries) { language ->
                ChoiceRow(
                    label = stringResource(language.label()),
                    selected = state.language == language,
                    onClick = { onIntent(SettingsIntent.SetLanguage(language)) },
                    modifier = Modifier.testTag(SettingsTestTags.language(language.name)),
                )
            }
            item { StitListItem(headline = stringResource(R.string.feature_settings_notifications), onClick = onOpenNotifications) }
            aboutSection(state = state, onOpenLink = onOpenLink)
            if (demo.available) demoSection(demo = demo, onDemoIntent = onDemoIntent)
        }
    }
}

private fun LazyListScope.section(title: Int) {
    item {
        Text(
            text = stringResource(title),
            modifier = Modifier.padding(start = StitTheme.spacing.md, top = StitTheme.spacing.md, bottom = StitTheme.spacing.xs),
            style = StitTheme.typography.bodySm,
            color = StitTheme.colors.muted,
        )
    }
}

// «О приложении»: версия, build и APP_ENV видны без входа; правовые документы — по ссылкам из requirements (05 L-3).
private fun LazyListScope.aboutSection(state: SettingsState, onOpenLink: (String) -> Unit) {
    section(R.string.feature_settings_about)
    item {
        StitListItem(
            headline = stringResource(R.string.feature_settings_version, state.version, state.build),
            supporting = stringResource(R.string.feature_settings_environment, state.env.name.lowercase()),
        )
    }
    val links = listOfNotNull(
        state.links.privacyPolicy?.let { R.string.feature_settings_privacy_policy to it },
        state.links.accountDeletion?.let { R.string.feature_settings_account_deletion to it },
        state.links.support?.let { R.string.feature_settings_support to it },
    )
    items(links, key = { it.second }) { (label, url) -> StitListItem(headline = stringResource(label), onClick = { onOpenLink(url) }) }
}

// Панель demo-режима есть только при API_MODE=mock: сценарий и задержка сети, чтобы увидеть состояния экранов живьём.
private fun LazyListScope.demoSection(demo: DemoPanelState, onDemoIntent: (DemoPanelIntent) -> Unit) {
    section(R.string.feature_settings_demo)
    items(demo.scenarios) { scenario ->
        ChoiceRow(
            label = scenario.name,
            selected = demo.scenario == scenario,
            onClick = { onDemoIntent(DemoPanelIntent.SelectScenario(scenario)) },
            modifier = Modifier.testTag(SettingsTestTags.scenario(scenario.name)),
        )
    }
    item {
        StitTextField(
            value = demo.latencyMs.toString(),
            onValueChange = { text -> text.toLongOrNull()?.let { onDemoIntent(DemoPanelIntent.SetLatency(it)) } },
            label = stringResource(R.string.feature_settings_demo_latency),
            modifier = Modifier.padding(horizontal = StitTheme.spacing.md).testTag(SettingsTestTags.DEMO_LATENCY),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StitListItem(headline = label, modifier = modifier, onClick = onClick, trailing = { RadioButton(selected = selected, onClick = null) })
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.System -> R.string.feature_settings_theme_system
    ThemeMode.Light -> R.string.feature_settings_theme_light
    ThemeMode.Dark -> R.string.feature_settings_theme_dark
}

private fun AppLanguage.label(): Int = when (this) {
    AppLanguage.System -> R.string.feature_settings_language_system
    AppLanguage.Ru -> R.string.feature_settings_language_ru
    AppLanguage.En -> R.string.feature_settings_language_en
}

internal fun MockScenario.testTag(): String = SettingsTestTags.scenario(name)
