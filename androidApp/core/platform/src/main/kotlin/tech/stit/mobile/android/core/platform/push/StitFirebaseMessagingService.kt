package tech.stit.mobile.android.core.platform.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tech.stit.mobile.SharedContainer
import javax.inject.Inject

@AndroidEntryPoint
class StitFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var container: SharedContainer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) = PushNotifier.show(this, PushMessage.parse(message.data, this))

    // Новый токен — перерегистрация на бэкенде сразу, не дожидаясь следующего старта (FCM 25.1: колбэк устаревший, но живой).
    @Deprecated("FCM 25.1 заменяет onNewToken на onRegistered; оставлено до перехода транспорта на register()")
    override fun onNewToken(token: String) {
        scope.launch { runCatching { container.pushRegistrar.register(force = true) } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
