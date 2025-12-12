package com.example.rsrtest.core.offline

import android.content.Context
import com.example.rsrtest.core.network.ConnectivityManager
import com.example.rsrtest.core.error.ErrorHandler
import com.example.rsrtest.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gestor centralizado para operaciones offline
 */
class OfflineManager(
    private val context: Context,
    private val connectivityManager: ConnectivityManager,
    private val errorHandler: ErrorHandler,
    private val productRepository: ProductRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()
    
    private val _pendingSyncItems = MutableStateFlow<List<SyncItem>>(emptyList())
    val pendingSyncItems: StateFlow<List<SyncItem>> = _pendingSyncItems.asStateFlow()
    
    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: StateFlow<SyncProgress?> = _syncProgress.asStateFlow()
    
    private val syncMutex = Mutex()
    
    data class SyncItem(
        val id: String,
        val type: SyncType,
        val data: Any,
        val timestamp: Long = System.currentTimeMillis(),
        val retryCount: Int = 0
    )
    
    enum class SyncType {
        PURCHASE_CREATE,
        PURCHASE_UPDATE,
        PRODUCT_UPDATE,
        STORE_UPDATE,
        DETECTION_LOG
    }
    
    data class SyncProgress(
        val totalItems: Int,
        val processedItems: Int,
        val currentOperation: String,
        val success: Boolean = true
    )
    
    init {
        // Observar cambios de conectividad
        scope.launch {
            connectivityManager.isConnected.collect { isConnected ->
                _isOfflineMode.value = !isConnected

                if (isConnected) {
                    // Cuando se recupera la conexión, sincronizar pendientes
                    synchronizePendingItems()
                }
            }
        }
        
        // Cargar items pendientes al inicio
        loadPendingSyncItems()
    }
    
    private fun loadPendingSyncItems() {
        // Implementar carga desde base de datos local
        // Por ahora, iniciamos con lista vacía
    }
    
    /**
     * Agrega un item para sincronización cuando haya conexión
     */
    suspend fun queueForSync(type: SyncType, data: Any): String {
        val syncItem = SyncItem(
            id = generateSyncId(),
            type = type,
            data = data
        )
        
        _pendingSyncItems.value = _pendingSyncItems.value + syncItem
        
        // Si hay conexión, intentar sincronizar inmediatamente
        if (connectivityManager.isOnline()) {
            synchronizePendingItems()
        }
        
        return syncItem.id
    }
    
    /**
     * Sincroniza todos los items pendientes
     */
    suspend fun synchronizePendingItems(): Boolean {
        if (_pendingSyncItems.value.isEmpty()) {
            return true
        }
        
        return syncMutex.withLock {
            val itemsToSync = _pendingSyncItems.value
            var successCount = 0
            var failureCount = 0
            
            _syncProgress.value = SyncProgress(
                totalItems = itemsToSync.size,
                processedItems = 0,
                currentOperation = "Iniciando sincronización..."
            )
            
            for ((index, item) in itemsToSync.withIndex()) {
                try {
                    _syncProgress.value = _syncProgress.value?.copy(
                        processedItems = index,
                        currentOperation = "Sincronizando ${item.type}"
                    )
                    
                    val success = when (item.type) {
                        SyncType.PURCHASE_CREATE -> syncPurchase(item)
                        SyncType.PURCHASE_UPDATE -> syncPurchaseUpdate(item)
                        SyncType.PRODUCT_UPDATE -> syncProductUpdate(item)
                        SyncType.STORE_UPDATE -> syncStoreUpdate(item)
                        SyncType.DETECTION_LOG -> syncDetectionLog(item)
                    }
                    
                    if (success) {
                        successCount++
                        removePendingSyncItem(item.id)
                    } else {
                        failureCount++
                        updateRetryCount(item.id)
                    }
                    
                } catch (e: Exception) {
                    failureCount++
                    updateRetryCount(item.id)
                    errorHandler.handleError(e, "sync_${item.type}")
                }
            }
            
            _syncProgress.value = _syncProgress.value?.copy(
                processedItems = itemsToSync.size,
                currentOperation = "Sincronización completada",
                success = failureCount == 0
            )
            
            // Limpiar estado de progreso después de 2 segundos
            kotlinx.coroutines.delay(2000)
            _syncProgress.value = null
            
            return failureCount == 0
        }
    }
    
    private suspend fun syncPurchase(item: SyncItem): Boolean {
        return errorHandler.withErrorHandling("sync_purchase") {
            val purchase = item.data as PurchaseHistory
            // Implementar sincronización con Firebase
            // firebaseService.savePurchase(purchase)
            true
        }.isSuccess
    }
    
    private suspend fun syncPurchaseUpdate(item: SyncItem): Boolean {
        return errorHandler.withErrorHandling("sync_purchase_update") {
            // Implementar actualización de compra
            true
        }.isSuccess
    }
    
    private suspend fun syncProductUpdate(item: SyncItem): Boolean {
        return errorHandler.withErrorHandling("sync_product_update") {
            val product = item.data as Product
            // Implementar sincronización de producto
            true
        }.isSuccess
    }
    
    private suspend fun syncStoreUpdate(item: SyncItem): Boolean {
        return errorHandler.withErrorHandling("sync_store_update") {
            val store = item.data as Store
            // Implementar sincronización de tienda
            true
        }.isSuccess
    }
    
    private suspend fun syncDetectionLog(item: SyncItem): Boolean {
        return errorHandler.withErrorHandling("sync_detection_log") {
            // Implementar sincronización de log de detecciones
            true
        }.isSuccess
    }
    
    private fun removePendingSyncItem(itemId: String) {
        _pendingSyncItems.value = _pendingSyncItems.value.filter { it.id != itemId }
    }
    
    private fun updateRetryCount(itemId: String) {
        _pendingSyncItems.value = _pendingSyncItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(retryCount = item.retryCount + 1)
            } else {
                item
            }
        }
    }
    
    private fun generateSyncId(): String {
        return "sync_${System.currentTimeMillis()}_${(0..1000).random()}"
    }
    
    /**
     * Verifica si una operación puede realizarse offline
     */
    fun canPerformOperationOffline(operationType: SyncType): Boolean {
        return when (operationType) {
            SyncType.PURCHASE_CREATE -> true
            SyncType.PURCHASE_UPDATE -> true
            SyncType.PRODUCT_UPDATE -> false // Requiere validación en línea
            SyncType.STORE_UPDATE -> false // Requiere validación en línea
            SyncType.DETECTION_LOG -> true
        }
    }
    
    /**
     * Obtiene estadísticas de sincronización
     */
    fun getSyncStats(): SyncStats {
        val pendingItems = _pendingSyncItems.value
        return SyncStats(
            pendingItems = pendingItems.size,
            hasFailedItems = pendingItems.any { it.retryCount > 3 },
            oldestPendingItem = pendingItems.minByOrNull { it.timestamp }?.timestamp,
            isOnline = connectivityManager.isOnline()
        )
    }
    
    data class SyncStats(
        val pendingItems: Int,
        val hasFailedItems: Boolean,
        val oldestPendingItem: Long?,
        val isOnline: Boolean
    )
    
    /**
     * Fuerza una sincronización manual
     */
    suspend fun forceSync(): Boolean {
        if (!connectivityManager.isOnline()) {
            errorHandler.handleCustomError(
                ErrorHandler.ErrorState.NetworkError
            )
            return false
        }
        
        return synchronizePendingItems()
    }
    
    /**
     * Limpia items pendientes (usar con cuidado)
     */
    suspend fun clearPendingSyncItems() {
        _pendingSyncItems.value = emptyList()
    }
}