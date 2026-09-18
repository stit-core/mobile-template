package tech.stit.mobile.android.core.platform.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import tech.stit.mobile.core.background.BackgroundCondition
import tech.stit.mobile.core.background.BackgroundScheduler
import tech.stit.mobile.core.background.BackgroundTask
import tech.stit.mobile.core.background.BackgroundTiming
import tech.stit.mobile.core.background.BackgroundTrigger
import java.util.concurrent.TimeUnit

// Механизм выбирается по timing: неточная работа — WorkManager с условиями, обещанный момент — будильник ради показа уведомления.
class AndroidBackgroundScheduler(private val context: Context) : BackgroundScheduler {
    private val alarms: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(task: BackgroundTask) {
        when (task.timing) {
            BackgroundTiming.Exact -> alarm(task)

            BackgroundTiming.Window, BackgroundTiming.Opportunistic -> work(task)

            // Длящаяся работа требует foreground-сервиса с постоянным индикатором — этот планировщик её не поднимает.
            BackgroundTiming.Continuous -> error("continuous-задача '${task.id}' планировщику не отдаётся")
        }
    }

    override fun cancel(id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(id)
        alarms.cancel(alarmIntent(id))
    }

    private fun work(task: BackgroundTask) {
        val data = workDataOf(BackgroundTaskWorker.KEY_TASK_ID to task.id)
        val manager = WorkManager.getInstance(context)
        if (task.trigger == BackgroundTrigger.Periodic) {
            val request = PeriodicWorkRequestBuilder<BackgroundTaskWorker>(task.periodMinutes, TimeUnit.MINUTES)
                .setConstraints(task.constraints())
                .setInputData(data)
                .build()
            manager.enqueueUniquePeriodicWork(task.id, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            val request = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
                .setConstraints(task.constraints())
                .setInputData(data)
                .build()
            manager.enqueueUniqueWork(task.id, ExistingWorkPolicy.REPLACE, request)
        }
    }

    private fun alarm(task: BackgroundTask) {
        val at = checkNotNull(task.at) { "exact-задача '${task.id}' без момента показа" }.toEpochMilliseconds()
        if (exactAllowed()) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarms, AlarmManager.RTC_WAKEUP, at, alarmIntent(task.id))
        } else {
            AlarmManagerCompat.setAndAllowWhileIdle(alarms, AlarmManager.RTC_WAKEUP, at, alarmIntent(task.id))
        }
    }

    // Начиная с Android 14 SCHEDULE_EXACT_ALARM по умолчанию не выдано: без него момент показа только приблизительный.
    private fun exactAllowed(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarms.canScheduleExactAlarms() else true

    private fun alarmIntent(taskId: String): PendingIntent {
        val intent = Intent(context, BackgroundAlarmReceiver::class.java).putExtra(BackgroundTaskWorker.KEY_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

private fun BackgroundTask.constraints(): Constraints = Constraints.Builder()
    .setRequiredNetworkType(networkType())
    .setRequiresCharging(BackgroundCondition.Charging in conditions)
    .build()

private fun BackgroundTask.networkType(): NetworkType = when {
    BackgroundCondition.Unmetered in conditions -> NetworkType.UNMETERED
    BackgroundCondition.Network in conditions || trigger == BackgroundTrigger.Connectivity -> NetworkType.CONNECTED
    else -> NetworkType.NOT_REQUIRED
}
