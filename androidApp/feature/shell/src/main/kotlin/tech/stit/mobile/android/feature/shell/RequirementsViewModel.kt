package tech.stit.mobile.android.feature.shell

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.requirements.RequirementsIntent
import tech.stit.mobile.feature.requirements.RequirementsState
import javax.inject.Inject

@HiltViewModel
class RequirementsViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.requirementsPresenter()
    val state: StateFlow<RequirementsState> = presenter.state

    init {
        presenter.dispatch(RequirementsIntent.Start)
    }

    fun dispatch(intent: RequirementsIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
