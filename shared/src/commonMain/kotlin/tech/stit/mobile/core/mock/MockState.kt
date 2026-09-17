package tech.stit.mobile.core.mock

import kotlinx.serialization.json.Json
import tech.stit.mobile.core.api.generated.AppRequirementsOutputBody
import tech.stit.mobile.core.api.generated.AuthUserView
import tech.stit.mobile.core.api.generated.DeviceListOutputBody
import tech.stit.mobile.core.api.generated.DeviceView
import tech.stit.mobile.core.api.generated.LoginOutputBody
import tech.stit.mobile.core.api.generated.SessionListOutputBody
import tech.stit.mobile.core.api.generated.SessionView
import tech.stit.mobile.core.api.generated.TodoListOutputBody
import tech.stit.mobile.core.api.generated.TodoView
import tech.stit.mobile.core.api.generated.UserMeOutputBody
import tech.stit.mobile.core.api.generated.UserView
import kotlin.time.Instant

// In-memory состояние сценария: мутации меняют его до следующего reset(), как @mswjs/data у SPA.
internal class MockState(
    var user: UserView,
    val login: LoginOutputBody,
    val todos: MutableList<TodoView>,
    val sessions: MutableList<SessionView>,
    val devices: MutableList<DeviceView>,
    var refreshToken: String?,
) {
    val usedRefreshTokens: MutableSet<String> = mutableSetOf()
    var issued: Int = 0
    var todoSeq: Int = todos.size

    companion object {
        fun load(scenario: MockScenario, json: Json): MockState {
            fun fixture(entity: String): String = Fixtures.files["${scenario.fixtureDir}/$entity"]
                ?: error("нет фикстуры mock/fixtures/${scenario.fixtureDir}/$entity.json")
            if (scenario == MockScenario.Error) return empty()
            return MockState(
                user = json.decodeFromString(UserMeOutputBody.serializer(), fixture("user")).data,
                login = json.decodeFromString(LoginOutputBody.serializer(), fixture("login")),
                todos = json.decodeFromString(TodoListOutputBody.serializer(), fixture("todos")).data.toMutableList(),
                sessions = json.decodeFromString(SessionListOutputBody.serializer(), fixture("sessions")).data.toMutableList(),
                devices = json.decodeFromString(DeviceListOutputBody.serializer(), fixture("devices")).data.toMutableList(),
                refreshToken = if (scenario.seedsSession) MockTransport.SEED_REFRESH_TOKEN else null,
            )
        }

        // Сценарий error не отвечает данными: состояние не нужно, но роутер требует объект.
        private fun empty(): MockState = MockState(
            user = UserView(
                createdAt = Instant.DISTANT_PAST,
                email = "",
                id = "",
                name = "",
                status = "",
                updatedAt = Instant.DISTANT_PAST,
            ),
            login = LoginOutputBody(user = AuthUserView(email = "", id = "", name = "", role = "")),
            todos = mutableListOf(),
            sessions = mutableListOf(),
            devices = mutableListOf(),
            refreshToken = null,
        )
    }
}
