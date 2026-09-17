package tech.stit.mobile.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import tech.stit.mobile.android.core.platform.NotificationPermissionGate
import tech.stit.mobile.android.feature.shell.ShellViewModel
import tech.stit.mobile.feature.shell.ShellIntent
import javax.inject.Inject

// AppCompatActivity: per-app locale ниже API 33 работает только с её контекстом; она же — FragmentActivity для BiometricPrompt.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var notificationPermission: NotificationPermissionGate

    private val shellViewModel: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationPermission.register(this)
        openLink(intent)
        setContent { StitApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openLink(intent)
    }

    // Ссылка идёт в презентер, а не в навигацию: auth-gate и pending route — его решение (rule navigation).
    private fun openLink(intent: Intent) {
        intent.dataString?.let { shellViewModel.dispatch(ShellIntent.OpenLink(it)) }
    }
}
