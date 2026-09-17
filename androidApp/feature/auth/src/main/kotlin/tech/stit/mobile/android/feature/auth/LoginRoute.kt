package tech.stit.mobile.android.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// После входа ничего не навигирует: ShellState.auth становится SignedIn, и app сам меняет корень стека.
@Composable
fun LoginRoute(onOpenSettings: () -> Unit, modifier: Modifier = Modifier, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(state = state, onIntent = viewModel::dispatch, onOpenSettings = onOpenSettings, modifier = modifier)
}
