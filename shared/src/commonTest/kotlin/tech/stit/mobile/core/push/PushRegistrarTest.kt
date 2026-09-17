package tech.stit.mobile.core.push

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import tech.stit.mobile.core.api.ApiJson
import tech.stit.mobile.core.api.DevicesApi
import tech.stit.mobile.core.api.generated.DeviceRegisterInputBody
import tech.stit.mobile.core.lib.SettingsKeys
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.testing.FakeClock
import tech.stit.mobile.testing.FakeLocale
import tech.stit.mobile.testing.FakeNotificationPermission
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.FakePushTransport
import tech.stit.mobile.testing.FakeSettings
import tech.stit.mobile.testing.MemoryLogSink
import tech.stit.mobile.testing.testApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class PushRegistrarTest {
    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<DeviceRegisterInputBody>()
    private val fcm = FakePushTransport(PushProvider.Fcm, "fcm-token-1")
    private val rustore = FakePushTransport(PushProvider.Rustore, null)
    private val permission = FakeNotificationPermission(granted = true)
    private val settings = FakeSettings()
    private val clock = FakeClock()
    private val puts get() = requests.count { it.method == HttpMethod.Put }
    private val deletes get() = requests.filter { it.method == HttpMethod.Delete }.map { it.url.encodedPath }

    private val engine = MockEngine { request ->
        requests += request
        if (request.method == HttpMethod.Put) {
            bodies += ApiJson.decodeFromString(DeviceRegisterInputBody.serializer(), request.body.toByteArray().decodeToString())
            val device = """{"id":"dev-$puts","provider":"fcm","platform":"android","app_version":"1.0.0+1",""" +
                """"created_at":"2026-09-17T00:00:00Z","last_seen_at":"2026-09-17T00:00:00Z"}"""
            respond("""{"data":$device}""", HttpStatusCode.OK, json)
        } else {
            respond("", HttpStatusCode.NoContent, json)
        }
    }

    private val registrar = PushRegistrar(
        api = DevicesApi(testApiClient(engine)),
        transports = listOf(fcm, rustore),
        permission = permission,
        appInfo = FakePlatformDeps().appInfo.copy(build = 1),
        locale = FakeLocale,
        settings = settings,
        sandbox = true,
        clock = clock,
        logger = Logger("t", MemoryLogSink()),
    )

    @Test
    fun registersEveryTransportWithTokenAndSkipsUnchangedRepeat() = runTest {
        assertEquals(1, registrar.register())
        val body = bodies.single()
        assertEquals(DeviceRegisterInputBody.Provider.Fcm, body.provider)
        assertEquals("fcm-token-1", body.token)
        assertEquals("1.0.0+1", body.appVersion)
        assertEquals("ru-RU", body.locale)
        assertEquals("Europe/Moscow", body.timezone)
        assertEquals(true, body.sandbox)
        assertEquals(true, body.pushEnabled)
        assertEquals(registrar.installationId(), body.installationId)

        assertEquals(0, registrar.register())
        assertEquals(1, puts)
        clock.current += 25.hours
        assertEquals(1, registrar.register())
        fcm.currentToken = "fcm-token-2"
        assertEquals(1, registrar.register())
        assertEquals(3, puts)
        assertEquals(1, registrar.register(force = true))
    }

    @Test
    fun unregisterDeletesKnownDevicesAndForgetsThem() = runTest {
        registrar.register()
        registrar.unregister()
        assertEquals(listOf("/api/v1/devices/dev-1"), deletes)
        assertNull(settings.values[SettingsKeys.PUSH_REGISTRATION_PREFIX + "fcm"])
        registrar.unregister()
        assertEquals(1, deletes.size)
    }

    @Test
    fun optInAsksPermissionThenRegisters() = runTest {
        permission.granted = false
        assertEquals(PushStatus.NotAsked, registrar.status())
        assertTrue(registrar.optIn())
        assertEquals(1, permission.requests)
        assertEquals(1, puts)
        assertEquals(PushStatus.Granted, registrar.status())
    }

    @Test
    fun deniedPermissionAndOptOutAreRemembered() = runTest {
        permission.granted = false
        permission.answer = false
        assertFalse(registrar.optIn())
        assertEquals(0, puts)
        assertEquals(PushStatus.Declined, registrar.status())

        permission.granted = true
        registrar.register()
        registrar.optOut()
        assertEquals(PushStatus.Declined, registrar.status())
        assertEquals(1, deletes.size)
    }

    @Test
    fun installationIdIsGeneratedOnceAndLooksLikeUuid() = runTest {
        val id = registrar.installationId()
        assertEquals(36, id.length)
        assertEquals(id, registrar.installationId())
        assertEquals(id, settings.values[SettingsKeys.INSTALLATION_ID])
    }
}
