package tech.stit.mobile.android.feature.shell

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.testing.FakeBiometricUnlock
import tech.stit.mobile.android.core.testing.FakePlatformDeps
import tech.stit.mobile.android.core.testing.MainDispatcherRule
import tech.stit.mobile.android.core.testing.demoConfig
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.deeplink.Route
import tech.stit.mobile.feature.shell.ShellIntent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Robolectric нужен ради FragmentActivity в сигнатуре BiometricUnlock; сеть не трогается — MockTransport.
@RunWith(AndroidJUnit4::class)
class ShellViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private fun container() = SharedContainer(FakePlatformDeps(filesDir = folder.root.absolutePath), demoConfig())

    @Test
    fun `старт восстанавливает сессию, ссылка ждёт входа и гасится RouteConsumed`() = runTest {
        val viewModel = ShellViewModel(container(), FakeBiometricUnlock(required = false))
        // FileCache читает на Dispatchers.IO — ждём результат старта, а не виртуальное время.
        assertEquals(AuthState.SignedOut, viewModel.state.first { it.auth != AuthState.Unknown }.auth)
        assertTrue(viewModel.unlocked.value)

        viewModel.dispatch(ShellIntent.OpenLink("https://app.demo.invalid/todos/todo-1"))
        assertEquals(Route.TodoDetail("todo-1"), viewModel.state.value.pendingRoute)
        viewModel.dispatch(ShellIntent.OpenLink("stit://settings"))
        assertEquals(Route.Settings, viewModel.state.value.route)
        viewModel.dispatch(ShellIntent.RouteConsumed)
        assertNull(viewModel.state.value.route)
    }

    @Test
    fun `биометрия требуется — замок до успешного ответа`() = runTest {
        val biometric = FakeBiometricUnlock(required = true, answer = false)
        val viewModel = ShellViewModel(container(), biometric)
        assertFalse(viewModel.unlocked.value)

        viewModel.unlock(FakeActivity())
        advanceUntilIdle()
        assertFalse(viewModel.unlocked.value)

        biometric.answer = true
        viewModel.unlock(FakeActivity())
        advanceUntilIdle()
        assertTrue(viewModel.unlocked.value)
        assertEquals(2, biometric.attempts)
    }
}

// Фейк не открывает окно: Activity нужна только как тип параметра.
private class FakeActivity : android.app.Activity()
