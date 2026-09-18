package tech.stit.mobile.android.core.platform.background

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import tech.stit.mobile.core.background.BackgroundCondition
import tech.stit.mobile.core.background.BackgroundTask
import tech.stit.mobile.core.background.BackgroundTiming
import tech.stit.mobile.core.background.BackgroundTrigger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class AndroidBackgroundSchedulerTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val alarms = application.getSystemService(AlarmManager::class.java)
    private val scheduler = AndroidBackgroundScheduler(application)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(application)
    }

    @Test
    fun `периодическая задача становится уникальной работой с условиями`() {
        scheduler.schedule(
            BackgroundTask(
                id = "refresh-todos",
                trigger = BackgroundTrigger.Periodic,
                timing = BackgroundTiming.Window,
                conditions = listOf(BackgroundCondition.Network),
                periodMinutes = PERIOD_MINUTES,
            ),
        )

        val info = workInfo("refresh-todos")
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
        assertEquals(PERIOD_MINUTES, checkNotNull(info.periodicityInfo).repeatIntervalMillis / MILLIS_PER_MINUTE)
    }

    @Test
    fun `условия unmetered и charging доезжают до WorkManager`() {
        scheduler.schedule(
            BackgroundTask(
                id = "sync-heavy",
                trigger = BackgroundTrigger.Connectivity,
                timing = BackgroundTiming.Opportunistic,
                conditions = listOf(BackgroundCondition.Unmetered, BackgroundCondition.Charging),
            ),
        )

        val info = workInfo("sync-heavy")
        assertEquals(NetworkType.UNMETERED, info.constraints.requiredNetworkType)
        assertTrue(info.constraints.requiresCharging())
    }

    @Test
    fun `точная задача ставит будильник на обещанный момент и ведёт в приёмник`() {
        val at = Instant.parse("2026-09-18T07:30:00Z")
        scheduler.schedule(
            BackgroundTask(
                id = "todo-reminder:todo-1",
                trigger = BackgroundTrigger.Time,
                timing = BackgroundTiming.Exact,
                at = at,
            ),
        )

        val alarm = shadowOf(alarms).scheduledAlarms.single()
        assertEquals(at.toEpochMilliseconds(), alarm.triggerAtTime)
        val saved = shadowOf(alarm.operation).savedIntent
        assertEquals(BackgroundAlarmReceiver::class.java.name, saved.component?.className)
        assertEquals("todo-reminder:todo-1", saved.getStringExtra(BackgroundTaskWorker.KEY_TASK_ID))
    }

    @Test
    fun `приёмник будильника отдаёт задачу воркеру`() {
        val at = Instant.parse("2026-09-18T07:30:00Z")
        val task = BackgroundTask("todo-reminder:todo-1", BackgroundTrigger.Time, BackgroundTiming.Exact, at = at)
        scheduler.schedule(task)

        val alarm = shadowOf(alarms).scheduledAlarms.single()
        BackgroundAlarmReceiver().onReceive(application, shadowOf(alarm.operation).savedIntent)

        val enqueued = WorkManager.getInstance(application).getWorkInfosByTag(BackgroundTaskWorker::class.java.name).get()
        assertEquals(1, enqueued.size)
    }

    @Test
    fun `отмена снимает и работу, и будильник`() {
        scheduler.schedule(BackgroundTask("refresh-todos", BackgroundTrigger.Periodic, BackgroundTiming.Window))
        scheduler.schedule(
            BackgroundTask(
                "todo-reminder:todo-1",
                BackgroundTrigger.Time,
                BackgroundTiming.Exact,
                at = Instant.parse("2026-09-18T07:30:00Z"),
            ),
        )

        scheduler.cancel("refresh-todos")
        scheduler.cancel("todo-reminder:todo-1")

        assertEquals(WorkInfo.State.CANCELLED, workInfo("refresh-todos").state)
        assertTrue(shadowOf(alarms).scheduledAlarms.isEmpty())
    }

    @Test
    fun `длящаяся задача планировщику не отдаётся`() {
        assertFailsWith<IllegalStateException> {
            scheduler.schedule(BackgroundTask("record", BackgroundTrigger.UserStarted, BackgroundTiming.Continuous))
        }
    }

    @Test
    fun `exact-задача без момента показа — ошибка конфигурации`() {
        assertFailsWith<IllegalStateException> {
            scheduler.schedule(BackgroundTask("todo-reminder:todo-1", BackgroundTrigger.Time, BackgroundTiming.Exact))
        }
    }

    private fun workInfo(id: String): WorkInfo = WorkManager.getInstance(application).getWorkInfosForUniqueWork(id).get().single()

    private companion object {
        const val PERIOD_MINUTES = 360L
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
