package tech.stit.mobile.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PushOptInRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: PushOptInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PushOptInScreen(state = state, onIntent = viewModel::dispatch, onBack = onBack, modifier = modifier)
}
