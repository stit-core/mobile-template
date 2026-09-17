package tech.stit.mobile.android.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Test
import tech.stit.mobile.core.deeplink.Route
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationTest {
    @Test
    fun `каждый Route отображается в Destination`() {
        val cases = mapOf(
            Route.Home to Destination.TodoList,
            Route.TodoList to Destination.TodoList,
            Route.Login to Destination.Login,
            Route.Profile to Destination.Profile,
            Route.Settings to Destination.Settings,
            Route.TodoDetail("todo-1") to Destination.TodoDetail("todo-1"),
        )
        cases.forEach { (route, destination) -> assertEquals(destination, route.toDestination(), route.toString()) }
    }

    @Test
    fun `корни вкладок — TodoList, Profile, Settings`() {
        assertTrue(Destination.TodoList.isTopLevel)
        assertTrue(Destination.Profile.isTopLevel)
        assertTrue(Destination.Settings.isTopLevel)
        listOf(Destination.Login, Destination.TodoDetail("1"), Destination.Sessions, Destination.DeleteAccount, Destination.PushOptIn)
            .forEach { assertFalse(it.isTopLevel, it.toString()) }
    }

    @Test
    fun `navigateTo не дублирует верх стека, resetTo меняет корень, pop не опускается ниже корня`() {
        val stack = NavBackStack<NavKey>(Destination.TodoList)
        stack.navigateTo(Destination.TodoDetail("1"))
        stack.navigateTo(Destination.TodoDetail("1"))
        assertEquals(listOf<NavKey>(Destination.TodoList, Destination.TodoDetail("1")), stack.toList())
        assertEquals(Destination.TodoDetail("1"), stack.current)

        assertTrue(stack.pop())
        assertFalse(stack.pop())
        assertEquals(listOf<NavKey>(Destination.TodoList), stack.toList())

        stack.resetTo(Destination.Settings)
        stack.resetTo(Destination.Settings)
        assertEquals(listOf<NavKey>(Destination.Settings), stack.toList())
    }
}
