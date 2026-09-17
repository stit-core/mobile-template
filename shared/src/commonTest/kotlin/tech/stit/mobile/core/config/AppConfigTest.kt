package tech.stit.mobile.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppConfigTest {
    @Test
    fun parsesRawStringsOfEnvProperties() {
        val config = AppConfig.parse(
            env = "demo",
            apiBaseUrl = "https://api.demo.invalid/",
            apiMode = "mock",
            authMode = "bearer",
            deepLinkHost = "app.demo.invalid",
            deepLinkScheme = "stit",
            sentryDsn = "",
        )
        val expected = AppConfig(AppEnv.Demo, "https://api.demo.invalid", ApiMode.Mock, AuthMode.Bearer, "app.demo.invalid", "stit", "")
        assertEquals(expected, config)
    }

    @Test
    fun modesParseCaseInsensitively() {
        val cases = listOf(
            "real" to ApiMode.Real,
            "MOCK" to ApiMode.Mock,
            "Mock" to ApiMode.Mock,
        )
        cases.forEach { (raw, expected) -> assertEquals(expected, ApiMode.parse(raw), raw) }
        assertEquals(AuthMode.Cookie, AuthMode.parse("COOKIE"))
        assertEquals(AuthMode.Bearer, AuthMode.parse("bearer"))
    }

    @Test
    fun unknownModeNamesAllowedValues() {
        val error = assertFailsWith<IllegalArgumentException> { ApiMode.parse("fake") }
        assertTrue(error.message!!.endsWith("допустимы: real, mock"), error.message)
        assertFailsWith<IllegalArgumentException> { AuthMode.parse("oauth") }
    }
}
