package tech.stit.mobile.android.core.platform.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

// Будильник отдаёт задачу воркеру: процесс уже разбужен, а работой владеет WorkManager, а не короткоживущий приёмник.
class BackgroundAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(BackgroundTaskWorker.KEY_TASK_ID) ?: return
        val request = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
            .setInputData(workDataOf(BackgroundTaskWorker.KEY_TASK_ID to id))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
