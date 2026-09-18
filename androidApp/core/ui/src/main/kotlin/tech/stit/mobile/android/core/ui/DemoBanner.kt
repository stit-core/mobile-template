package tech.stit.mobile.android.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

// Приложение идёт на фикстурах по флагу demo_login: пользователь обязан видеть, что данные ненастоящие, и
// выходить одним нажатием — иначе demo-режим не отличить от сломанного бэкенда.
@Composable
fun DemoBanner(onExit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StitTheme.colors.warning)
            .padding(start = StitTheme.spacing.md, end = StitTheme.spacing.xs)
            .testTag(UiTestTags.DEMO_BANNER),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.core_ui_demo_banner),
            modifier = Modifier.weight(1f),
            color = StitTheme.colors.appBg,
            style = StitTheme.typography.bodySm,
        )
        TextButton(onClick = onExit) { Text(text = stringResource(R.string.core_ui_demo_exit), color = StitTheme.colors.appBg) }
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoBannerPreview() {
    StitTheme { DemoBanner(onExit = {}) }
}
