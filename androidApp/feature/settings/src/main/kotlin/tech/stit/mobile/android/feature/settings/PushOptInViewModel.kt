package tech.stit.mobile.android.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.push.PushOptInIntent
import tech.stit.mobile.feature.push.PushOptInState
import javax.inject.Inject

@HiltViewModel
class PushOptInViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.pushOptInPresenter()
    val state: StateFlow<PushOptInState> = presenter.state

    init {
        presenter.dispatch(PushOptInIntent.Start)
    }

    fun dispatch(intent: PushOptInIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
