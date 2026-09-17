package tech.stit.mobile.android.core.platform

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Биометрия — разблокировка приложения при живой сессии (rule auth-and-session), не замена входа; включается AUTH_BIOMETRIC_UNLOCK.
interface BiometricUnlock {
    val required: Boolean

    suspend fun authenticate(activity: Activity): Boolean
}

class DefaultBiometricUnlock(private val context: Context, enabled: Boolean) : BiometricUnlock {
    override val required: Boolean =
        enabled && BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    // BiometricPrompt хостится только FragmentActivity (AppCompatActivity приложения) — иной хост означает «не разблокировано».
    override suspend fun authenticate(activity: Activity): Boolean {
        val host = activity as? FragmentActivity ?: return false
        return prompt(host)
    }

    private suspend fun prompt(activity: FragmentActivity): Boolean = suspendCancellableCoroutine { continuation ->
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (continuation.isActive) continuation.resume(false)
            }
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.core_platform_biometric_title))
            .setSubtitle(context.getString(R.string.core_platform_biometric_subtitle))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }

    private companion object {
        // Биометрия класса 3 или код устройства: без своей кнопки «отмена» — её даёт DEVICE_CREDENTIAL.
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
