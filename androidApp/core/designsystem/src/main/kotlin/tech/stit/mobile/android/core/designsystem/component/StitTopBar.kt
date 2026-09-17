package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.R
import tech.stit.mobile.android.core.designsystem.StitIcons
import tech.stit.mobile.android.core.designsystem.StitTheme

// TopAppBar Material 3 всё ещё experimental: opt-in живёт в одном месте, экраны его не видят.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(StitIcons.ArrowBack),
                        contentDescription = stringResource(R.string.core_designsystem_back),
                    )
                }
            }
        },
        actions = actions,
    )
}

@Preview(showBackground = true)
@Composable
private fun StitTopBarPreview() {
    StitTheme { StitTopBar(title = "Задачи", onBack = {}) }
}
