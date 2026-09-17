package tech.stit.mobile.android.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Презентеры shared живут на Dispatchers.Main: в JVM-тесте его подменяет StandardTestDispatcher (как Compose test v2).
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
