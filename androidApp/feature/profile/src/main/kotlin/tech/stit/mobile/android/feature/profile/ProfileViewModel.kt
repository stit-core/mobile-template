package tech.stit.mobile.android.feature.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.profile.ProfileIntent
import tech.stit.mobile.feature.profile.ProfileState
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.profilePresenter()
    val state: StateFlow<ProfileState> = presenter.state

    init {
        presenter.dispatch(ProfileIntent.Start)
    }

    fun dispatch(intent: ProfileIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
