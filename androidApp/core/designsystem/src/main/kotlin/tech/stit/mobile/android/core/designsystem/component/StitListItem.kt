package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

@Composable
fun StitListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(text = headline) },
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        supportingContent = supporting?.let { { Text(text = it) } },
        leadingContent = leading,
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = StitTheme.colors.appBg),
    )
}

@Preview(showBackground = true)
@Composable
private fun StitListItemPreview() {
    StitTheme {
        StitListItem(headline = "Headline", supporting = "Supporting text", onClick = {})
    }
}
