package tech.stit.mobile.android.feature.shell

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.BiometricUnlock
import tech.stit.mobile.feature.shell.ShellIntent
import tech.stit.mobile.feature.shell.ShellState
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(container: SharedContainer, private val biometric: BiometricUnlock) : ViewModel() {
    private val presenter = container.shellPresenter()
    val state: StateFlow<ShellState> = presenter.state

    // Замок — состояние процесса, не сессии: после успешной биометрии до перезапуска не спрашиваем.
    private val _unlocked = MutableStateFlow(!biometric.required)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    init {
        presenter.dispatch(ShellIntent.Start)
    }

    fun dispatch(intent: ShellIntent) = presenter.dispatch(intent)

    fun unlock(activity: Activity) {
        viewModelScope.launch { if (biometric.authenticate(activity)) _unlocked.value = true }
    }

    override fun onCleared() = presenter.close()
}
