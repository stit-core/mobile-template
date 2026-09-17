package tech.stit.mobile.entity

import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.core.api.ApiError
import tech.stit.mobile.core.auth.AuthState
import tech.stit.mobile.core.auth.SessionStore
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.mock.MockScenario
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.signIn
import tech.stit.mobile.testing.testContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoriesTest {
    @Test
    fun userRepositoryLoadsCachesAndReportsUserId() = runTest {
        val platform = FakePlatformDeps()
        val fileSystem = FakeFileSystem()
        val container = testContainer(platform = platform, fileSystem = fileSystem)
        container.signIn()
        assertNull(container.userRepository.me.value)
        val me = container.userRepository.refresh()
        assertEquals("u-1", me.id)
        assertEquals(listOf<String?>("u-1"), platform.telemetry.userIds)

        val restarted = testContainer(platform = platform, fileSystem = fileSystem)
        restarted.userRepository.restore()
        assertEquals(me, restarted.userRepository.me.value)
    }

    @Test
    fun updateNameIsOptimisticAndRollsBackOnFailure() = runTest {
        val container = testContainer()
        container.signIn()
        val before = container.userRepository.refresh()
        val renamed = container.userRepository.updateName("Новое имя")
        assertEquals("Новое имя", renamed.name)
        assertEquals(before.version + 1, renamed.version)

        container.mockTransport!!.reset(MockScenario.Error)
        assertFailsWith<ApiError.Http> { container.userRepository.updateName("Не сохранится") }
        assertEquals("Новое имя", container.userRepository.me.value?.name)
    }

    @Test
    fun passwordChangeAndAccountDeletionNeedALoadedUser() = runTest {
        val container = testContainer()
        container.signIn()
        assertFailsWith<IllegalStateException> { container.userRepository.changePassword("old", "new") }
        container.userRepository.refresh()
        container.userRepository.changePassword("old", "new")
        container.userRepository.deleteAccount()
        container.userRepository.clear()
        assertNull(container.userRepository.me.value)
    }

    @Test
    fun todoRepositoryIsSingleSourceOfTruth() = runTest {
        val fileSystem = FakeFileSystem()
        val platform = FakePlatformDeps()
        val container = testContainer(platform = platform, fileSystem = fileSystem)
        container.signIn()
        assertEquals(3, container.todoRepository.refresh().size)
        val created = container.todoRepository.create("Четвёртая")
        assertEquals(4, container.todoRepository.todos.value.size)
        assertEquals(created, container.todoRepository.todos.value.first())
        assertTrue(container.todoRepository.toggle(created.id).done)
        assertEquals(created.id, container.todoRepository.get(created.id).id)
        container.todoRepository.delete(created.id)
        assertEquals(3, container.todoRepository.todos.value.size)

        val restarted = testContainer(platform = platform, fileSystem = fileSystem)
        restarted.todoRepository.restore()
        assertEquals(container.todoRepository.todos.value, restarted.todoRepository.todos.value)
    }

    @Test
    fun sessionRepositoryLoginAndLogoutDriveAuthStateAndSecrets() = runTest {
        val platform = FakePlatformDeps(notificationPermission = tech.stit.mobile.testing.FakeNotificationPermission(granted = true))
        val container = testContainer(platform = platform)
        assertEquals(AuthState.Unknown, container.sessionRepository.authState.value)
        container.signIn()
        assertEquals(AuthState.SignedIn, container.sessionRepository.authState.value)
        assertNotNull(platform.secureStore.values[SessionStore.REFRESH_TOKEN_KEY])
        container.pushRegistrar.register()
        assertNotNull(platform.settings.values[SettingsKeys.PUSH_REGISTRATION_PREFIX + "fcm"])

        container.sessionRepository.logout()
        assertEquals(AuthState.SignedOut, container.sessionRepository.authState.value)
        assertTrue(platform.secureStore.values.isEmpty())
        assertNull(platform.settings.values[SettingsKeys.PUSH_REGISTRATION_PREFIX + "fcm"])
    }

    @Test
    fun sessionsAndDevicesCanBeListedAndRevoked() = runTest {
        val container = testContainer()
        container.signIn()
        assertEquals(listOf("session-current", "session-web"), container.sessionRepository.sessions().map { it.jti })
        container.sessionRepository.revoke("session-web")
        assertEquals(listOf("session-current"), container.sessionRepository.sessions().map { it.jti })
        assertEquals(listOf("dev-1"), container.deviceRepository.list().map { it.id })
        container.deviceRepository.unregister("dev-1")
        assertTrue(container.deviceRepository.list().isEmpty())
        container.sessionRepository.revokeAll()
        assertEquals(AuthState.SignedOut, container.sessionRepository.authState.value)
    }

    @Test
    fun signOutClearsEveryCache() = runTest {
        val container = testContainer()
        container.signIn()
        container.userRepository.refresh()
        container.todoRepository.refresh()
        container.sessionStore.clear()
        assertNull(container.userRepository.me.value)
        assertTrue(container.todoRepository.todos.value.isEmpty())
    }
}
