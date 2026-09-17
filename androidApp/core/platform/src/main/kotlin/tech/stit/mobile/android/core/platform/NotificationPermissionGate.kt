package tech.stit.mobile.android.core.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import tech.stit.mobile.core.push.NotificationPermission
import javax.inject.Inject
import javax.inject.Singleton

// POST_NOTIFICATIONS живёт в Activity Result API: Activity регистрирует launcher в onCreate, презентер ждёт ответ через suspend.
@Singleton
class NotificationPermissionGate @Inject constructor(@ApplicationContext private val context: Context) : NotificationPermission {
    private var launcher: ActivityResultLauncher<String>? = null
    private var pending: CompletableDeferred<Boolean>? = null

    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending?.complete(granted)
            pending = null
        }
    }

    override suspend fun isGranted(): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return enabled
        return enabled &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    // До Android 13 разрешения нет: уведомления включает пользователь в системных настройках.
    override suspend fun request(): Boolean = when {
        isGranted() -> true
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && launcher != null -> ask(checkNotNull(launcher))
        else -> false
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun ask(launcher: ActivityResultLauncher<String>): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return answer.await()
    }
}
