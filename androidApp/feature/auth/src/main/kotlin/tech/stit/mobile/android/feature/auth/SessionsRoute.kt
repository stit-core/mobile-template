package tech.stit.mobile.android.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionsRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SessionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionsScreen(state = state, onIntent = viewModel::dispatch, onBack = onBack, modifier = modifier)
}
