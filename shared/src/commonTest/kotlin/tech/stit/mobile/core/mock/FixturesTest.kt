package tech.stit.mobile.core.mock

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import tech.stit.mobile.core.api.generated.AppRequirementsOutputBody
import tech.stit.mobile.core.api.generated.DeviceListOutputBody
import tech.stit.mobile.core.api.generated.ErrorModel
import tech.stit.mobile.core.api.generated.LoginOutputBody
import tech.stit.mobile.core.api.generated.SessionListOutputBody
import tech.stit.mobile.core.api.generated.TodoListOutputBody
import tech.stit.mobile.core.api.generated.UserMeOutputBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Контракт фикстур (07 §3, mock-check): каждая парсится сгенерированной моделью строго — лишнее поле ломает тест.
class FixturesTest {
    private val strict = Json { ignoreUnknownKeys = false }
    private val models: Map<String, KSerializer<*>> = mapOf(
        "user" to UserMeOutputBody.serializer(),
        "login" to LoginOutputBody.serializer(),
        "todos" to TodoListOutputBody.serializer(),
        "sessions" to SessionListOutputBody.serializer(),
        "devices" to DeviceListOutputBody.serializer(),
        "requirements" to AppRequirementsOutputBody.serializer(),
        "error" to ErrorModel.serializer(),
    )

    @Test
    fun everyFixtureParsesStrictlyWithGeneratedModels() {
        assertTrue(Fixtures.files.isNotEmpty())
        Fixtures.files.forEach { (key, json) ->
            val serializer = assertNotNull(models[key.substringAfter('/')], "нет модели для фикстуры $key")
            strict.decodeFromString(serializer, json)
        }
    }

    @Test
    fun fixtureDirectoriesMatchScenarios() {
        val directories = Fixtures.files.keys.map { it.substringBefore('/') }.toSet()
        assertEquals(MockScenario.entries.map { it.fixtureDir }.toSet(), directories)
    }

    @Test
    fun dataScenariosHaveEveryEntityAndErrorOnlyTheProblem() {
        val data = setOf("user", "login", "todos", "sessions", "devices", "requirements")
        MockScenario.entries.forEach { scenario ->
            val entities = Fixtures.files.keys.filter { it.startsWith(scenario.fixtureDir + "/") }.map { it.substringAfter('/') }.toSet()
            assertEquals(if (scenario == MockScenario.Error) setOf("error") else data, entities, scenario.name)
        }
    }
}
