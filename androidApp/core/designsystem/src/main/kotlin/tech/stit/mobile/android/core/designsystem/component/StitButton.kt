package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

@Composable
fun StitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: StitButtonVariant = StitButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val secondary = variant == StitButtonVariant.Secondary
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = StitTheme.spacing.touchTarget),
        // На время запроса кнопка не кликабельна: повторная отправка формы исключена без логики в экране.
        enabled = enabled && !loading,
        shape = StitTheme.shapes.control,
        colors = if (secondary) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(),
        border = if (secondary) ButtonDefaults.outlinedButtonBorder(enabled = enabled && !loading) else null,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(StitTheme.spacing.md),
                color = LocalContentColor.current,
                strokeWidth = StitTheme.spacing.xs / 2,
            )
        } else {
            Text(text = text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StitButtonPreview() {
    StitTheme {
        Column(modifier = Modifier.padding(StitTheme.spacing.md)) {
            StitButton(text = "Primary", onClick = {})
            StitButton(text = "Secondary", onClick = {}, variant = StitButtonVariant.Secondary)
            StitButton(text = "Loading", onClick = {}, loading = true)
        }
    }
}
