package tech.stit.mobile.entity

import tech.stit.mobile.core.api.generated.DeviceView
import tech.stit.mobile.core.api.generated.SessionView
import tech.stit.mobile.core.api.generated.TodoView
import tech.stit.mobile.core.api.generated.UserView
import tech.stit.mobile.entity.device.Device
import tech.stit.mobile.entity.device.toModel
import tech.stit.mobile.entity.session.Session
import tech.stit.mobile.entity.session.toModel
import tech.stit.mobile.entity.todo.Todo
import tech.stit.mobile.entity.todo.toModel
import tech.stit.mobile.entity.user.User
import tech.stit.mobile.entity.user.toModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MappersTest {
    private val t0 = Instant.parse("2026-09-01T00:00:00Z")
    private val t1 = Instant.parse("2026-09-02T00:00:00Z")

    @Test
    fun userViewMapsWithVersionFallback() {
        val cases = listOf(
            UserView(t0, "a@b", "u1", "Аня", "active", t1, 4L) to User("u1", "a@b", "Аня", "active", t0, t1, 4),
            UserView(t0, "a@b", "u1", "Аня", "active", t1, null) to User("u1", "a@b", "Аня", "active", t0, t1, 1),
        )
        cases.forEach { (dto, model) -> assertEquals(model, dto.toModel()) }
    }

    @Test
    fun todoSessionAndDeviceViewsMapFieldByField() {
        assertEquals(Todo("t1", "x", true, t0, t1, 2), TodoView(t0, true, "t1", "x", t1, 2).toModel())
        assertEquals(
            Session("j1", "android", "Pixel", "1.0.0+1", t0, t1, null, true),
            SessionView(true, t1, t0, "j1", "1.0.0+1", SessionView.Client.Android, "Pixel", null).toModel(),
        )
        assertEquals(
            Device("d1", "fcm", "android", "1.0.0+1", null, t0, t1, null),
            DeviceView("1.0.0+1", t0, "d1", t1, "android", "fcm", null, null).toModel(),
        )
    }
}
