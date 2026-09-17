package tech.stit.mobile.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppEnvTest {
    @Test
    fun parsesEveryFlavorNameCaseInsensitively() {
        val cases = mapOf(
            "dev" to AppEnv.Dev,
            "staging" to AppEnv.Staging,
            "prod" to AppEnv.Prod,
            "demo" to AppEnv.Demo,
            "PROD" to AppEnv.Prod,
        )
        cases.forEach { (raw, expected) -> assertEquals(expected, AppEnv.parse(raw), "parse(\"$raw\")") }
    }

    @Test
    fun rejectsUnknownEnvironment() {
        val error = assertFailsWith<IllegalArgumentException> { AppEnv.parse("local") }
        assertEquals("неизвестная среда 'local'; допустимы: dev, staging, prod, demo", error.message)
    }
}
