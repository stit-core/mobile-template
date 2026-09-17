package tech.stit.mobile.android.core.platform.push

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService
import tech.stit.mobile.SharedContainer
import javax.inject.Inject

@AndroidEntryPoint
class StitRuStoreMessagingService : RuStoreMessagingService() {
    @Inject
    lateinit var container: SharedContainer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) = PushNotifier.show(this, PushMessage.parse(message.data, this))

    override fun onNewToken(token: String) {
        scope.launch { runCatching { container.pushRegistrar.register(force = true) } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
