package tech.stit.mobile.core.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeepLinkParserTest {
    private val parser = DeepLinkParser(host = "app.demo.invalid", scheme = "stit")

    @Test
    fun urlsMapToRoutes() {
        val cases = mapOf(
            "https://app.demo.invalid/todos/todo-42" to Route.TodoDetail("todo-42"),
            "https://app.demo.invalid/todos" to Route.TodoList,
            "https://app.demo.invalid/todos/" to Route.TodoList,
            "https://app.demo.invalid/" to Route.Home,
            "https://app.demo.invalid" to Route.Home,
            "https://app.demo.invalid/profile?utm_source=push" to Route.Profile,
            "https://APP.DEMO.INVALID/settings" to Route.Settings,
            "https://app.demo.invalid/login" to Route.Login,
            "stit://todos/todo-42" to Route.TodoDetail("todo-42"),
            "stit://todos" to Route.TodoList,
            "stit://settings" to Route.Settings,
        )
        cases.forEach { (url, route) -> assertEquals(route, parser.parse(url), url) }
    }

    @Test
    fun foreignOrMalformedLinksAreIgnored() {
        val rejected = listOf(
            "https://other.invalid/todos/1",
            "http://app.demo.invalid/todos",
            "https://app.demo.invalid/unknown",
            "https://app.demo.invalid/todos/1/extra",
            "acme://todos/1",
            "not a url",
            "",
        )
        rejected.forEach { url -> assertEquals(null, parser.parse(url), url) }
    }

    @Test
    fun onlyLoginAndSettingsAreOpenBeforeSignIn() {
        assertFalse(Route.Login.requiresAuth)
        assertFalse(Route.Settings.requiresAuth)
        listOf(Route.Home, Route.Profile, Route.TodoList, Route.TodoDetail("1")).forEach { assertTrue(it.requiresAuth, it.toString()) }
    }
}
