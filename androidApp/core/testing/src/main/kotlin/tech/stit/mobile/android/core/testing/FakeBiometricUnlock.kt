package tech.stit.mobile.android.core.testing

import android.app.Activity
import tech.stit.mobile.android.core.platform.BiometricUnlock

class FakeBiometricUnlock(override val required: Boolean = false, var answer: Boolean = true) : BiometricUnlock {
    var attempts = 0

    override suspend fun authenticate(activity: Activity): Boolean {
        attempts++
        return answer
    }
}
