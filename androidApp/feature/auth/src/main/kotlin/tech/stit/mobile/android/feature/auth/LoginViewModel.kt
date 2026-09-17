package tech.stit.mobile.android.feature.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.auth.LoginIntent
import tech.stit.mobile.feature.auth.LoginState
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.loginPresenter()
    val state: StateFlow<LoginState> = presenter.state

    fun dispatch(intent: LoginIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
