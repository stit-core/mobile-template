package tech.stit.mobile.android.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    onOpenSessions: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onIntent = viewModel::dispatch,
        onOpenSessions = onOpenSessions,
        onOpenDeleteAccount = onOpenDeleteAccount,
        modifier = modifier,
    )
}
