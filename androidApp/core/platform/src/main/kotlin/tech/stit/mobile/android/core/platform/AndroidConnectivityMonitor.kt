package tech.stit.mobile.android.core.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.stit.mobile.core.lib.ConnectivityMonitor

// «Онлайн» = у активной сети есть интернет и она проверена системой (VALIDATED), иначе captive portal считался бы сетью.
class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val manager: ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val _online = MutableStateFlow(manager.isOnline())
    override val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        manager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    _online.value = networkCapabilities.isOnline()
                }

                override fun onLost(network: Network) {
                    _online.value = false
                }
            },
        )
    }
}

private fun ConnectivityManager.isOnline(): Boolean = activeNetwork?.let(::getNetworkCapabilities)?.isOnline() == true

private fun NetworkCapabilities.isOnline(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
