package com.example.rsrtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rsrtest.data.DetectedProduct
import com.example.rsrtest.data.MissingProduct
import com.example.rsrtest.data.Product
import com.example.rsrtest.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class DetectionViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _currentProducts = MutableStateFlow<Set<String>>(emptySet())
    val currentProducts: StateFlow<Set<String>> = _currentProducts.asStateFlow()

    private val _pepsicoDetections = MutableStateFlow<List<DetectedProduct>>(emptyList())
    val pepsicoDetections: StateFlow<List<DetectedProduct>> = _pepsicoDetections.asStateFlow()

    private val _missingProducts = MutableStateFlow<List<MissingProduct>>(emptyList())
    val missingProducts: StateFlow<List<MissingProduct>> = _missingProducts.asStateFlow()

    private val _currentProduct = MutableStateFlow<DetectedProduct?>(null)
    val currentProduct: StateFlow<DetectedProduct?> = _currentProduct.asStateFlow()

    private val _isCaptureMode = MutableStateFlow(false)
    val isCaptureMode: StateFlow<Boolean> = _isCaptureMode.asStateFlow()

    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()

    // Cooldown para evitar múltiples detecciones del mismo producto
    private val productCooldowns = mutableMapOf<String, Long>()
    private val cooldownPeriod = 3000L // 3 segundos

    fun processDetection(className: String, confidence: Float) {
        val normalizedName = className.lowercase()
        
        // Verificar cooldown
        if (!canAddProduct(normalizedName)) {
            Log.d("DetectionViewModel", "⏭️ Saltando detección por cooldown: $normalizedName")
            return
        }

        // Crear detección
        val detectedProduct = DetectedProduct(
            name = className,
            confidence = confidence
        )

        // Actualizar producto actual
        _currentProduct.value = detectedProduct

        // Agregar a lista de detecciones
        val currentList = _pepsicoDetections.value.toMutableList()
        currentList.add(detectedProduct)
        _pepsicoDetections.value = currentList

        // Actualizar contador
        _detectionCount.value = currentList.size

        // Actualizar productos actuales
        val currentSet = _currentProducts.value.toMutableSet()
        currentSet.add(normalizedName)
        _currentProducts.value = currentSet

        // Marcar producto con cooldown
        markProductAsAdded(normalizedName)

        // Si no está en modo captura manual, agregar automáticamente al carrito
        if (!_isCaptureMode.value) {
            searchAndAddToCart(normalizedName)
        }

        Log.d("DetectionViewModel", "✅ Detección procesada: $className (${confidence})")
    }

    private fun searchAndAddToCart(productName: String) {
        viewModelScope.launch {
            try {
                val foundProduct = productRepository.findProductByDetectionKeyword(productName)
                foundProduct?.let { product ->
                    addProductToCart(product)
                    Log.d("DetectionViewModel", "🛒 Producto agregado automáticamente: ${product.name}")
                } ?: run {
                    Log.d("DetectionViewModel", "❌ Producto no encontrado en BD: $productName")
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "Error buscando producto: ${e.message}")
            }
        }
    }

    private suspend fun addProductToCart(product: Product) {
        try {
            productRepository.addToCart(product.id, 1)
            Log.d("DetectionViewModel", "✅ Producto agregado al carrito: ${product.name}")
        } catch (e: Exception) {
            Log.e("DetectionViewModel", "Error agregando al carrito: ${e.message}")
        }
    }

    fun toggleCaptureMode() {
        _isCaptureMode.value = !_isCaptureMode.value
        Log.d("DetectionViewModel", "🔄 Modo captura: ${if (_isCaptureMode.value) "MANUAL" else "AUTOMÁTICO"}")
    }

    fun addCurrentProductToCart() {
        _currentProduct.value?.let { detection ->
            viewModelScope.launch {
                searchAndAddToCart(detection.name.lowercase())
            }
        }
    }

    private fun canAddProduct(productName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastAdded = productCooldowns[productName] ?: 0
        return (currentTime - lastAdded) > cooldownPeriod
    }

    private fun markProductAsAdded(productName: String) {
        productCooldowns[productName] = System.currentTimeMillis()
        cleanupOldCooldowns()
    }

    private fun cleanupOldCooldowns() {
        val currentTime = System.currentTimeMillis()
        val iterator = productCooldowns.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if ((currentTime - entry.value) > (cooldownPeriod * 2)) {
                iterator.remove()
            }
        }
    }

    fun clearDetections() {
        _pepsicoDetections.value = emptyList()
        _currentProducts.value = emptySet()
        _currentProduct.value = null
        _detectionCount.value = 0
        productCooldowns.clear()
        Log.d("DetectionViewModel", "🧹 Detecciones limpiadas")
    }

    fun removeProduct(productId: String) {
        val currentList = _pepsicoDetections.value.toMutableList()
        val removedProduct = currentList.find { it.id == productId }
        
        if (removedProduct != null) {
            currentList.remove(removedProduct)
            _pepsicoDetections.value = currentList
            _detectionCount.value = currentList.size
            
            // Actualizar productos actuales
            val currentSet = _currentProducts.value.toMutableSet()
            currentSet.remove(removedProduct.name.lowercase())
            _currentProducts.value = currentSet
            
            Log.d("DetectionViewModel", "🗑️ Producto removido: ${removedProduct.name}")
        }
    }
}