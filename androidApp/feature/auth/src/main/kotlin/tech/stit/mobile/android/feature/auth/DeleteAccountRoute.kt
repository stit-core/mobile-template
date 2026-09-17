package tech.stit.mobile.android.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// После удаления сессия закрыта презентером: auth становится SignedOut, app сам показывает вход.
@Composable
fun DeleteAccountRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: DeleteAccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeleteAccountScreen(state = state, onIntent = viewModel::dispatch, onBack = onBack, modifier = modifier)
}
