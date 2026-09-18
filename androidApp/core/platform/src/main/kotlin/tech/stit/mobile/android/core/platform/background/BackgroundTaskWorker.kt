package tech.stit.mobile.android.core.platform.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.stit.mobile.SharedContainer
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher

// Единственная точка исполнения фоновых задач: и окно WorkManager, и будильник приводят сюда.
class BackgroundTaskWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val entryPoint = entryPoint()
        val runner = inputData.getString(KEY_TASK_ID)?.let { entryPoint.sharedContainer().backgroundRunner(it) }
            ?: return Result.failure()
        return runner.run().deliver(applicationContext, entryPoint.widgetRefresher())
    }

    // Воркер создаёт WorkManager, а не Hilt: граф приложения достаётся entry point'ом.
    private fun entryPoint(): BackgroundEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, BackgroundEntryPoint::class.java)

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackgroundEntryPoint {
    fun sharedContainer(): SharedContainer

    fun widgetRefresher(): WidgetRefresher
}
