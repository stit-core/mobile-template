package tech.stit.mobile.android.core.platform.push

import android.app.Application
import tech.stit.mobile.core.push.PushTransport

// PUSH_PROVIDERS из config/env («rustore,fcm») → транспорты в этом порядке; неизвестное имя — ошибка конфигурации, не тихий пропуск.
fun pushTransports(application: Application, providers: String, rustoreProjectId: String): List<PushTransport> = providers.split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .map { name ->
        when (name) {
            "fcm" -> FcmTransport(application)
            "rustore" -> RuStoreTransport(application, rustoreProjectId)
            else -> error("PUSH_PROVIDERS: неизвестный провайдер '$name' (допустимы fcm, rustore)")
        }
    }
