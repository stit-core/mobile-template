package tech.stit.mobile.android.core.platform.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.stit.mobile.core.push.PushProvider
import tech.stit.mobile.core.push.PushTransport
import kotlin.coroutines.resume

class FcmTransport(private val context: Context) : PushTransport {
    override val provider: PushProvider = PushProvider.Fcm

    // Без google-services.json FirebaseApp не инициализируется: FCM недоступен, регистрация идёт по остальным транспортам.
    // FCM 25.1 объявил getToken() устаревшим в пользу register()/onRegistered; запрос токена по требованию остаётся рабочим.
    @Suppress("DEPRECATION")
    override suspend fun token(): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                continuation.resume(if (task.isSuccessful) task.result else null)
            }
        }
    }
}
