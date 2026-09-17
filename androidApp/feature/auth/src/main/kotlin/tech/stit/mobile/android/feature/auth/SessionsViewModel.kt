package tech.stit.mobile.android.feature.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.auth.SessionsIntent
import tech.stit.mobile.feature.auth.SessionsState
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.sessionsPresenter()
    val state: StateFlow<SessionsState> = presenter.state

    init {
        presenter.dispatch(SessionsIntent.Refresh)
    }

    fun dispatch(intent: SessionsIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
