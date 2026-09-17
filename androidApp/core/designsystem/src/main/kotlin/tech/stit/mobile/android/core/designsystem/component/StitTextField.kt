package tech.stit.mobile.android.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import tech.stit.mobile.android.core.designsystem.StitTheme

@Composable
fun StitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    secret: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(text = label) },
        supportingText = error?.let { { Text(text = it) } },
        isError = error != null,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = StitTheme.shapes.control,
    )
}

@Preview(showBackground = true)
@Composable
private fun StitTextFieldPreview() {
    StitTheme {
        Column(modifier = Modifier.padding(StitTheme.spacing.md)) {
            StitTextField(value = "demo@example.com", onValueChange = {}, label = "Email")
            StitTextField(value = "", onValueChange = {}, label = "Password", error = "Required", secret = true)
        }
    }
}
