package tech.stit.mobile.android.feature.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.auth.DeleteAccountIntent
import tech.stit.mobile.feature.auth.DeleteAccountState
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.deleteAccountPresenter()
    val state: StateFlow<DeleteAccountState> = presenter.state

    fun dispatch(intent: DeleteAccountIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
