package tech.stit.mobile.android.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(
    onOpenNotifications: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Тема применяется на корне приложения тем же экземпляром: презентер живёт на Activity, а не на записи back stack.
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = checkNotNull(LocalActivity.current) as ViewModelStoreOwner),
    demoViewModel: DemoPanelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val demo by demoViewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        demo = demo,
        onIntent = viewModel::dispatch,
        onDemoIntent = demoViewModel::dispatch,
        onOpenNotifications = onOpenNotifications,
        onOpenLink = onOpenLink,
        modifier = modifier,
    )
}
