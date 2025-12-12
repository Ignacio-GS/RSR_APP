package com.example.rsrtest.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestor de conectividad con monitoreo en tiempo real
 */
class ConnectivityManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _connectionType = MutableStateFlow(ConnectionType.NONE)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()
    
    private val _isMetered = MutableStateFlow(false)
    val isMetered: StateFlow<Boolean> = _isMetered.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateConnectionState()
        }
        
        override fun onLost(network: Network) {
            updateConnectionState()
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateConnectionState()
        }
    }
    
    enum class ConnectionType {
        NONE, WIFI, CELLULAR, ETHERNET, OTHER
    }
    
    init {
        updateConnectionState()
        registerNetworkCallback()
    }
    
    private fun registerNetworkCallback() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(
                networkRequest,
                networkCallback
            )
        } catch (e: Exception) {
            // Fallback a verificación periódica
            startPeriodicCheck()
        }
    }
    
    private fun startPeriodicCheck() {
        // Verificar cada 5 segundos como fallback
        scope.launch {
            while (true) {
                delay(5000)
                updateConnectionState()
            }
        }
    }
    
    private fun updateConnectionState() {
        val currentNetwork = connectivityManager.activeNetworkInfo
        
        _isConnected.value = currentNetwork?.isConnected == true
        
        if (_isConnected.value) {
            val capabilities = connectivityManager.getNetworkCapabilities(
                connectivityManager.activeNetwork
            )
            
            _connectionType.value = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 
                    ConnectionType.WIFI
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> 
                    ConnectionType.CELLULAR
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> 
                    ConnectionType.ETHERNET
                else -> ConnectionType.OTHER
            }
            
            _isMetered.value = !(capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
            ) ?: false)
        } else {
            _connectionType.value = ConnectionType.NONE
            _isMetered.value = false
        }
    }
    
    fun isOnline(): Boolean {
        return _isConnected.value
    }
    
    fun getConnectionType(): ConnectionType {
        return _connectionType.value
    }
    
    fun isOnWifi(): Boolean {
        return _connectionType.value == ConnectionType.WIFI
    }
    
    fun isOnCellular(): Boolean {
        return _connectionType.value == ConnectionType.CELLULAR
    }
    
    fun isConnectionMetered(): Boolean {
        return _isMetered.value
    }
    
    fun hasGoodConnection(): Boolean {
        return _isConnected.value && (
            _connectionType.value == ConnectionType.WIFI ||
            _connectionType.value == ConnectionType.ETHERNET
        )
    }
    
    suspend fun waitForConnection(timeoutMs: Long = 10000): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (_isConnected.value) {
                return true
            }
            delay(100)
        }

        return false
    }
    
    fun destroy() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignorar error al unregister
        }
    }
}