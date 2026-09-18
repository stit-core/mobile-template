package tech.stit.mobile.android.core.platform.background

import android.content.Context
import androidx.work.ListenableWorker
import tech.stit.mobile.android.core.platform.push.PushMessage
import tech.stit.mobile.android.core.platform.push.PushNotifier
import tech.stit.mobile.android.core.platform.widget.WidgetRefresher
import tech.stit.mobile.core.background.BackgroundRejection
import tech.stit.mobile.core.background.BackgroundResult

// Результат фоновой работы доносится до пользователя; отказ по ошибке отдаётся WorkManager на повтор с его backoff.
internal suspend fun BackgroundResult.deliver(context: Context, widgets: WidgetRefresher): ListenableWorker.Result = when (this) {
    is BackgroundResult.Notify -> {
        PushNotifier.show(context, toPushMessage(context))
        ListenableWorker.Result.success()
    }

    is BackgroundResult.RefreshWidget -> {
        widgets.refresh(slug)
        ListenableWorker.Result.success()
    }

    BackgroundResult.FreshData -> ListenableWorker.Result.success()

    is BackgroundResult.Rejected ->
        if (reason == BackgroundRejection.Failed) ListenableWorker.Result.retry() else ListenableWorker.Result.success()
}

// Локальное уведомление собирается тем же payload'ом, что push: реестр текстов, канал и дедуп — один (PushMessage).
private fun BackgroundResult.Notify.toPushMessage(context: Context): PushMessage = PushMessage.parse(
    buildMap {
        put("category", category)
        put("title_key", titleKey)
        put("body_key", bodyKey)
        put("notification_id", notificationId)
        if (args.isNotEmpty()) put("args", args.joinToString("|"))
        deepLink?.let { put("deep_link", it) }
    },
    context,
)
