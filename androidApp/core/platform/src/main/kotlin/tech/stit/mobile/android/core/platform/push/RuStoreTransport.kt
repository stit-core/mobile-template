package tech.stit.mobile.android.core.platform.push

import android.app.Application
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.rustore.sdk.pushclient.RuStorePushClient
import tech.stit.mobile.core.push.PushProvider
import tech.stit.mobile.core.push.PushTransport
import kotlin.coroutines.resume

// Пустой RUSTORE_PROJECT_ID — транспорт выключен (шаблон без проекта в консоли RuStore); SDK инициализируется при старте процесса.
class RuStoreTransport(application: Application, private val projectId: String) : PushTransport {
    override val provider: PushProvider = PushProvider.Rustore

    init {
        if (projectId.isNotBlank() && !RuStorePushClient.isInitialized) RuStorePushClient.init(application, projectId)
    }

    override suspend fun token(): String? {
        if (projectId.isBlank()) return null
        return suspendCancellableCoroutine { continuation ->
            RuStorePushClient.getToken()
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
