package tech.stit.mobile.core.requirements

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import tech.stit.mobile.core.api.ApiJson
import tech.stit.mobile.core.api.AppApi
import tech.stit.mobile.core.api.generated.AppLinksView
import tech.stit.mobile.core.api.generated.AppRequirementsView
import tech.stit.mobile.core.lib.FileCache
import tech.stit.mobile.core.observability.Logger
import tech.stit.mobile.testing.FakeClock
import tech.stit.mobile.testing.FakePlatformDeps
import tech.stit.mobile.testing.MemoryLogSink
import tech.stit.mobile.testing.TestDispatchers
import tech.stit.mobile.testing.testApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class RequirementsClientTest {
    private val json = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val ok = """{"data":{"status":"update_required","min_version":"2.0.0","latest_version":"2.1.0","store_url":"https://play.invalid/app",
        |"flags":{"demo_login":"true"},"links":{"privacy_policy":"https://app.invalid/privacy"}}}
    """.trimMargin()
    private var status = HttpStatusCode.OK
    private var body = ok
    private var calls = 0
    private var lastRequest: HttpRequestData? = null
    private val clock = FakeClock()
    private val fileSystem = FakeFileSystem()
    private val appInfo = FakePlatformDeps().appInfo

    private fun client(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler): RequirementsClient {
        val engine = MockEngine { request ->
            calls++
            lastRequest = request
            respond(body, status, json)
        }
        val cache = FileCache(
            "/cache/requirements.json".toPath(),
            AppRequirements.serializer(),
            ApiJson,
            fileSystem,
            TestDispatchers(StandardTestDispatcher(scheduler)),
        )
        return RequirementsClient(AppApi(testApiClient(engine)), cache, appInfo, clock, Logger("t", MemoryLogSink()))
    }

    @Test
    fun fetchIdentifiesPlatformVersionBuildAndStore() = runTest {
        client(testScheduler).fetch()
        val query = assertNotNull(lastRequest).url.parameters
        assertEquals("android", query["platform"])
        assertEquals("1.0.0", query["version"])
        assertEquals("1000000", query["build"])
        assertEquals("direct", query["store"])
    }

    @Test
    fun fetchMapsResponseAndCachesIt() = runTest {
        val client = client(testScheduler)
        val requirements = client.fetch()
        assertEquals(RequirementsStatus.UpdateRequired, requirements.status)
        assertEquals("https://play.invalid/app", requirements.storeUrl)
        assertEquals(true, requirements.flag("demo_login"))
        assertEquals(false, requirements.flag("missing"))
        assertEquals("https://app.invalid/privacy", requirements.links.privacyPolicy)
        assertEquals(requirements, client.current.value)

        val restored = client(testScheduler)
        restored.restore()
        assertEquals(requirements, restored.current.value)
    }

    @Test
    fun fetchIsThrottledToOnceInFiveMinutes() = runTest {
        val client = client(testScheduler)
        client.fetch()
        client.fetch()
        assertEquals(1, calls)
        clock.current += 6.minutes
        client.fetch()
        assertEquals(2, calls)
        client.fetch(force = true)
        assertEquals(3, calls)
    }

    @Test
    fun backendWithoutEndpointYieldsDefault() = runTest {
        status = HttpStatusCode.NotFound
        body = """{"code":"NOT_FOUND"}"""
        assertEquals(AppRequirements.Default, client(testScheduler).fetch())
    }

    @Test
    fun serverFailurePrefersCacheOverDefault() = runTest {
        val client = client(testScheduler)
        val cached = client.fetch()
        status = HttpStatusCode.InternalServerError
        body = """{"code":"INTERNAL"}"""
        clock.current += 10.minutes
        assertEquals(cached, client.fetch())
        assertEquals(AppRequirements.Default, client(testScheduler).also { fileSystem.delete("/cache/requirements.json".toPath()) }.fetch())
    }

    @Test
    fun everyStatusMapsToModel() {
        val cases = mapOf(
            AppRequirementsView.Status.Ok to RequirementsStatus.Ok,
            AppRequirementsView.Status.UpdateRecommended to RequirementsStatus.UpdateRecommended,
            AppRequirementsView.Status.UpdateRequired to RequirementsStatus.UpdateRequired,
            AppRequirementsView.Status.Maintenance to RequirementsStatus.Maintenance,
        )
        cases.forEach { (dto, model) ->
            val view = AppRequirementsView(emptyMap(), "1", AppLinksView(), "1", dto, Instant.parse("2026-09-17T00:00:00Z"), null)
            assertEquals(model, view.toModel().status)
            assertEquals(Instant.parse("2026-09-17T00:00:00Z"), view.toModel().maintenanceUntil)
        }
    }
}
