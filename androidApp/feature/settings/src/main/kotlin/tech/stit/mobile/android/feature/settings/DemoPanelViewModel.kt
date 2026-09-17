package tech.stit.mobile.android.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.feature.demo.DemoPanelIntent
import tech.stit.mobile.feature.demo.DemoPanelState
import javax.inject.Inject

@HiltViewModel
class DemoPanelViewModel @Inject constructor(container: SharedContainer) : ViewModel() {
    private val presenter = container.demoPanelPresenter()
    val state: StateFlow<DemoPanelState> = presenter.state

    init {
        presenter.dispatch(DemoPanelIntent.Start)
    }

    fun dispatch(intent: DemoPanelIntent) = presenter.dispatch(intent)

    override fun onCleared() = presenter.close()
}
