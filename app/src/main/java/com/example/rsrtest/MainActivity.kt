package com.example.rsrtest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rsrtest.ui.theme.RSRTESTTheme
import com.example.rsrtest.data.*
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Data class para productos detectados
data class DetectedProduct(
    val name: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
)

// Data class para productos faltantes
data class MissingProduct(
    val name: String,
    val category: String,
    val confidence: Float, // qué tan seguro estamos de que falta
    val reason: String, // por qué creemos que falta
    val expectedLocation: String? = null
)

// Enum para tipos de contexto
enum class ShelfContext(val displayName: String) {
    REFRIGERATOR("Refrigerador"),
    SNACK_SHELF("Estante de Snacks"),
    BEVERAGE_COOLER("Enfriador de Bebidas"),
    CHECKOUT_AREA("Área de Checkout"),
    CANDY_SECTION("Sección de Dulces"),
    UNKNOWN("Desconocido")
}

// Elemento del carrito de compras (legacy - reemplazado por CartItemWithProduct)
data class CartItem(
    val name: String,
    val quantity: Int = 1
)

// Colores del tema PepsiCo
object PepsiColors {
    val PepsiBlue = Color(0xFF004B93)
    val PepsiRed = Color(0xFFE32934)
    val PepsiWhite = Color(0xFFFFFFFF)
    val DarkBlue = Color(0xFF002F6C)
    val LightBlue = Color(0xFF0065BD)
    val BackgroundDark = Color(0xFF0A1929)
    val CardDark = Color(0xFF1E3A5F)
    val AccentOrange = Color(0xFFFF6B35)
    val SuccessGreen = Color(0xFF4CAF50)
}

// ===== FUNCIONES AUXILIARES =====

fun vibrateOnDetection(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}

fun shareDetections(context: Context, products: List<DetectedProduct>) {
    val shareText = buildString {
        appendLine("🔍 Productos PepsiCo Detectados:")
        appendLine()

        val groupedProducts = products.groupBy { it.name }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        groupedProducts.forEach { (product, count) ->
            appendLine("• $product: $count vez(veces)")
        }

        appendLine()
        appendLine("Total: ${products.size} detecciones")
        appendLine("📱 Escaneado con PepsiCo Scanner")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Compartir detecciones")
    context.startActivity(shareIntent)
}

class MainActivity : ComponentActivity() {

    // Firestore
    private lateinit var db: FirebaseFirestore

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Camera Executor
    private lateinit var cameraExecutor: ExecutorService

    // Room Database
    private lateinit var database: AppDatabase
    private lateinit var productRepository: ProductRepository

    // YOLO Detector (usando TensorFlow Lite temporalmente)
    private val yoloDetector by lazy {
        YoloDetectorTfLite(this)
    }

    // Estado para productos detectados
    private val _detectedProducts = MutableStateFlow<List<DetectedProduct>>(emptyList())
    val detectedProducts: StateFlow<List<DetectedProduct>> = _detectedProducts.asStateFlow()

    // Estado para el producto actual destacado
    private val _currentProduct = MutableStateFlow<DetectedProduct?>(null)
    val currentProduct: StateFlow<DetectedProduct?> = _currentProduct.asStateFlow()

    // Contador de detecciones
    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()
    
    // Estados para controlar la captura manual - INICIA EN MANUAL para evitar spam
    private val _isCaptureMode = MutableStateFlow(true)  // CAMBIO: Inicia en modo MANUAL
    val isCaptureMode: StateFlow<Boolean> = _isCaptureMode.asStateFlow()
    
    private val _lastCaptureTime = MutableStateFlow(0L)
    private val _captureButtonText = MutableStateFlow("📸")
    val captureButtonText: StateFlow<String> = _captureButtonText.asStateFlow()
    
    // Sistema de cooldown por producto para evitar duplicados
    private val productCooldowns = mutableMapOf<String, Long>()
    private val PRODUCT_COOLDOWN_MS = 3000L // 3 segundos de cooldown por producto
    
    // Cooldown global para evitar spam masivo
    private var lastGlobalAdd = 0L
    private val GLOBAL_COOLDOWN_MS = 1000L // 1 segundo entre cualquier adición
    
    // Throttling para análisis de imagen
    private var lastAnalysisTime = 0L
    private val ANALYSIS_THROTTLE_MS = 2000L // Solo analizar cada 2 segundos (anti-spam)
    
    // Throttling para adición a lista de detectados
    private var lastDetectionListUpdate = 0L
    private val DETECTION_LIST_THROTTLE_MS = 2000L // Solo actualizar lista cada 2 segundos
    
    // Productos agregados recientemente (para UI feedback)
    private val _recentlyAddedProducts = MutableStateFlow<Set<String>>(emptySet())
    val recentlyAddedProducts: StateFlow<Set<String>> = _recentlyAddedProducts.asStateFlow()
    
    // SISTEMA ANTI-SPAM DRÁSTICO: Productos ya agregados en esta sesión
    private val productsAddedInSession = mutableSetOf<String>()
    private val MAX_SAME_PRODUCT_PER_SESSION = 1  // Solo UNA vez por sesión
    
    // SISTEMA DE PRODUCTOS FALTANTES
    private val _missingProducts = MutableStateFlow<List<MissingProduct>>(emptyList())
    val missingProducts: StateFlow<List<MissingProduct>> = _missingProducts.asStateFlow()
    
    private val _currentContext = MutableStateFlow(ShelfContext.UNKNOWN)
    val currentContext: StateFlow<ShelfContext> = _currentContext.asStateFlow()

    // Carrito de compras (legacy)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Carrito de compras con Room
    private val _cartItemsRoom = MutableStateFlow<List<CartItemWithProduct>>(emptyList())
    val cartItemsRoom: StateFlow<List<CartItemWithProduct>> = _cartItemsRoom.asStateFlow()

    // Navegación
    private val _currentScreen = MutableStateFlow("scanner")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Búsqueda y filtros
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    // Historial de compras
    private val _purchaseHistory = MutableStateFlow<List<PurchaseHistory>>(emptyList())
    val purchaseHistory: StateFlow<List<PurchaseHistory>> = _purchaseHistory.asStateFlow()

    // Tiendas
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _selectedStore = MutableStateFlow<Store?>(null)
    val selectedStore: StateFlow<Store?> = _selectedStore.asStateFlow()

    private val _storeSearchQuery = MutableStateFlow("")
    val storeSearchQuery: StateFlow<String> = _storeSearchQuery.asStateFlow()

    private val _filteredStores = MutableStateFlow<List<Store>>(emptyList())
    val filteredStores: StateFlow<List<Store>> = _filteredStores.asStateFlow()

    private fun addToCart(productName: String) {
        // Método legacy
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == productName }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(productName, 1))
        }
        _cartItems.value = current

        // Método con Room
        lifecycleScope.launch {
            val product = productRepository.findProductByDetectionKeyword(productName.lowercase())
            product?.let {
                productRepository.addToCart(it.id)
                Log.d("Cart", "Agregado al carrito: ${it.name}")
            }
        }
    }
    
    // Nueva función más directa para agregar producto al carrito
    private fun addProductToCart(product: Product) {
        Log.d("MainActivity", "🛒 Agregando producto al carrito: ${product.name}")
        
        // Método legacy
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == product.name }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
            Log.d("MainActivity", "🛒 Cantidad incrementada: ${product.name} -> ${item.quantity + 1}")
        } else {
            current.add(CartItem(product.name, 1))
            Log.d("MainActivity", "🛒 Producto nuevo agregado: ${product.name}")
        }
        _cartItems.value = current

        // Método con Room
        lifecycleScope.launch {
            try {
                productRepository.addToCart(product.id)
                Log.d("MainActivity", "🛒 Agregado al carrito Room: ${product.name}")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error agregando al carrito Room: ${e.message}")
            }
        }
    }
    
    // Función DRÁSTICA para verificar si un producto puede ser agregado
    private fun canAddProduct(productName: String): Boolean {
        
        // 🚫 REGLA 1: Solo una vez por sesión (pero permitir después de 10 segundos para testing)
        if (productsAddedInSession.contains(productName)) {
            val currentTime = System.currentTimeMillis()
            val lastAdd = productCooldowns[productName] ?: 0L
            if (currentTime - lastAdd < 10000L) { // 10 segundos en lugar de toda la sesión
                Log.d("MainActivity", "🚫 BLOQUEADO: '$productName' en cooldown de 10s")
                return false
            } else {
                // Remover de la sesión después de 10 segundos
                productsAddedInSession.remove(productName)
                Log.d("MainActivity", "🔄 '$productName' removido de sesión después de 10s")
            }
        }
        
        val currentTime = System.currentTimeMillis()
        
        // 🚫 REGLA 2: Cooldown global mínimo
        val timeSinceGlobal = currentTime - lastGlobalAdd
        if (timeSinceGlobal < GLOBAL_COOLDOWN_MS) {
            Log.d("MainActivity", "🚫 BLOQUEADO: Cooldown GLOBAL activo (${timeSinceGlobal}ms < ${GLOBAL_COOLDOWN_MS}ms)")
            return false
        }
        
        // 🚫 REGLA 3: Cooldown específico del producto (doble verificación)
        val lastAdded = productCooldowns[productName] ?: 0L
        val timeSinceLastAdded = currentTime - lastAdded
        if (timeSinceLastAdded < PRODUCT_COOLDOWN_MS) {
            val remainingTime = PRODUCT_COOLDOWN_MS - timeSinceLastAdded
            Log.d("MainActivity", "🚫 BLOQUEADO: '$productName' en cooldown (faltan ${remainingTime}ms)")
            return false
        }
        
        Log.d("MainActivity", "✅ PERMITIDO: '$productName' puede ser agregado")
        return true
    }
    
    // Función para marcar un producto como agregado recientemente
    private fun markProductAsAdded(productName: String) {
        val currentTime = System.currentTimeMillis()
        productCooldowns[productName] = currentTime
        lastGlobalAdd = currentTime
        
        // 🔒 AGREGAR A SESIÓN - Solo una vez por sesión
        productsAddedInSession.add(productName)
        Log.d("MainActivity", "🔒 '$productName' marcado como agregado en esta sesión. Total: ${productsAddedInSession.size}")
        
        // Agregar a la lista de productos recientes para UI
        val current = _recentlyAddedProducts.value.toMutableSet()
        current.add(productName)
        _recentlyAddedProducts.value = current
        
        // Remover de la lista después de 3 segundos
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            val updated = _recentlyAddedProducts.value.toMutableSet()
            updated.remove(productName)
            _recentlyAddedProducts.value = updated
        }
        
        Log.d("MainActivity", "Producto marcado como agregado: $productName (cooldown hasta ${currentTime + PRODUCT_COOLDOWN_MS})")
        
        // Limpiar cooldowns antiguos para evitar acumulación de memoria
        cleanupOldCooldowns()
    }
    
    // Función para limpiar cooldowns antiguos
    private fun cleanupOldCooldowns() {
        val currentTime = System.currentTimeMillis()
        val iterator = productCooldowns.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > PRODUCT_COOLDOWN_MS * 2) { // Limpiar después de 2x el cooldown
                iterator.remove()
            }
        }
    }
    
    // Función para resetear la sesión de productos agregados (útil para testing)
    private fun resetProductSession() {
        productsAddedInSession.clear()
        productCooldowns.clear()
        lastGlobalAdd = 0L
        Log.d("MainActivity", "🔄 Sesión de productos reseteada - se pueden volver a agregar productos")
        runOnUiThread {
            Toast.makeText(this, "🔄 Sesión reseteada", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Función para resetear la sesión anti-spam
    fun resetAntiSpamSession() {
        productsAddedInSession.clear()
        productCooldowns.clear()
        lastGlobalAdd = 0L
        Log.d("MainActivity", "🔄 Sesión anti-spam reseteada - todos los productos pueden agregarse nuevamente")
        runOnUiThread {
            Toast.makeText(this, "🔄 Sesión reseteada - productos desbloqueados", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Función para activar el modo captura
    fun toggleCaptureMode() {
        _isCaptureMode.value = !_isCaptureMode.value
        Log.d("MainActivity", "🔄 Modo cambiado a: ${if (_isCaptureMode.value) "MANUAL" else "AUTOMÁTICO"}")
        runOnUiThread {
            Toast.makeText(this, 
                if (_isCaptureMode.value) "🔴 Modo MANUAL activado" else "🔵 Modo AUTOMÁTICO activado", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Función para capturar manualmente
    fun captureNow() {
        val currentTime = System.currentTimeMillis()
        val cooldownTime = 2000L // 2 segundos de cooldown
        
        if (currentTime - _lastCaptureTime.value < cooldownTime) {
            Log.d("MainActivity", "Captura en cooldown, espera...")
            return
        }
        
        _lastCaptureTime.value = currentTime
        _captureButtonText.value = "⏳"
        
        // Capturar las detecciones actuales
        val currentDetections = _detectedProducts.value
        if (currentDetections.isNotEmpty()) {
            // Agregar solo la detección con mayor confianza
            val bestDetection = currentDetections.maxByOrNull { it.confidence }
            bestDetection?.let { detection ->
                // Buscar en BD antes de agregar
                lifecycleScope.launch {
                    val foundProduct = productRepository.findProductByDetectionKeyword(detection.name.lowercase())
                    foundProduct?.let { product ->
                        if (canAddProduct(product.name)) {
                            addToCart(product.name)
                            markProductAsAdded(product.name)
                            Log.d("MainActivity", "Capturado manualmente: ${detection.name}")
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "📸 ${product.name} capturado", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "⏳ ${product.name} ya fue agregado recientemente", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } ?: run {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "❌ Producto no encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } ?: run {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "⚠️ No hay detecciones para capturar", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Resetear el texto del botón después de un delay
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000)
            _captureButtonText.value = "📸"
        }
    }
    
    // MOTOR DE ANÁLISIS DE PRODUCTOS FALTANTES
    
    // Base de datos de productos esperados por contexto - SOLO PEPSICO
    private fun getExpectedProductsByContext(context: ShelfContext): Map<String, List<String>> {
        return when (context) {
            ShelfContext.REFRIGERATOR, ShelfContext.BEVERAGE_COOLER -> mapOf(
                "Colas PepsiCo" to listOf("Pepsi", "Pepsi Black"),
                "Cítricos PepsiCo" to listOf("7up", "Squirt"),
                "Sabores PepsiCo" to listOf("Mirinda", "Manzanita Sol"),
                "Agua PepsiCo" to listOf("Aquafina", "H2OH!"),
                "Deportivas PepsiCo" to listOf("Gatorade")
            )
            ShelfContext.SNACK_SHELF -> mapOf(
                "Papas PepsiCo" to listOf("Sabritas", "Ruffles"),
                "Queso PepsiCo" to listOf("Cheetos", "Doritos"),
                "Maíz PepsiCo" to listOf("Fritos", "Tostitos"),
                "Dulces PepsiCo" to listOf("Emperador")
            )
            ShelfContext.CHECKOUT_AREA -> mapOf(
                "Bebidas PepsiCo" to listOf("7up", "Pepsi", "Aquafina"),
                "Snacks PepsiCo" to listOf("Cheetos", "Doritos", "Sabritas")
            )
            else -> mapOf()
        }
    }
    
    // Inferir contexto basado en productos detectados
    private fun inferContext(detectedProducts: List<DetectedProduct>): ShelfContext {
        val productNames = detectedProducts.map { it.name.lowercase() }
        
        val beverageCount = productNames.count { 
            it.contains("pepsi") || it.contains("7up") || it.contains("mirinda") || 
            it.contains("squirt") || it.contains("agua") || it.contains("aquafina")
        }
        
        val snackCount = productNames.count { 
            it.contains("cheetos") || it.contains("sabritas") || it.contains("doritos") || 
            it.contains("emperador") || it.contains("ruffles")
        }
        
        return when {
            beverageCount > snackCount && beverageCount >= 2 -> ShelfContext.BEVERAGE_COOLER
            snackCount > beverageCount && snackCount >= 2 -> ShelfContext.SNACK_SHELF
            beverageCount > 0 && snackCount > 0 -> ShelfContext.CHECKOUT_AREA
            else -> ShelfContext.UNKNOWN
        }
    }
    
    // Análizar productos faltantes basado en contexto
    private fun analyzeMissingProducts(detectedProducts: List<DetectedProduct>): List<MissingProduct> {
        val context = inferContext(detectedProducts)
        _currentContext.value = context
        
        if (context == ShelfContext.UNKNOWN) {
            return emptyList()
        }
        
        val expectedCategories = getExpectedProductsByContext(context)
        val detectedNames = detectedProducts.map { it.name.lowercase() }
        val missingProducts = mutableListOf<MissingProduct>()
        
        for ((category, expectedProducts) in expectedCategories) {
            val presentInCategory = expectedProducts.filter { expected ->
                detectedNames.any { detected -> 
                    detected.contains(expected.lowercase()) || expected.lowercase().contains(detected)
                }
            }
            
            val missingInCategory = expectedProducts - presentInCategory.toSet()
            
            // Si hay algunos productos de la categoría, es probable que falten los otros
            if (presentInCategory.isNotEmpty() && missingInCategory.isNotEmpty()) {
                missingInCategory.forEach { missing ->
                    val confidence = when {
                        presentInCategory.size >= 2 -> 0.8f  // Alta confianza
                        presentInCategory.size == 1 -> 0.6f  // Media confianza
                        else -> 0.3f  // Baja confianza
                    }
                    
                    missingProducts.add(
                        MissingProduct(
                            name = missing,
                            category = category,
                            confidence = confidence,
                            reason = "Presentes en $category: ${presentInCategory.joinToString(", ")}",
                            expectedLocation = context.displayName
                        )
                    )
                }
            }
        }
        
        // Ordenar por confianza descendente
        return missingProducts.sortedByDescending { it.confidence }.take(5) // Máximo 5 sugerencias
    }

    private fun updateCartItem(productName: String, delta: Int) {
        // Método legacy
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == productName }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty > 0) {
                current[index] = item.copy(quantity = newQty)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    // Nuevos métodos para Room
    private fun updateCartItemRoom(cartItem: CartItemWithProduct, delta: Int) {
        lifecycleScope.launch {
            val newQuantity = cartItem.quantity + delta
            productRepository.updateCartItemQuantity(cartItem.product.id, newQuantity)
        }
    }

    private fun removeFromCart(productName: String) {
        // Método legacy
        val current = _cartItems.value.toMutableList()
        current.removeAll { it.name == productName }
        _cartItems.value = current
    }

    private fun removeFromCartRoom(cartItem: CartItemWithProduct) {
        lifecycleScope.launch {
            productRepository.removeFromCart(cartItem.product.id)
        }
    }

    // Estado del permiso de cámara
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    // Inicializar datos de productos en Room
    private fun initializeProductData() {
        lifecycleScope.launch {
            // Verificar si ya hay productos en la base de datos
            val existingProducts = database.productDao().getAllProducts()
            existingProducts.collect { products ->
                if (products.isEmpty()) {
                    // Insertar productos iniciales
                    productRepository.insertProducts(ProductData.getAllPepsiCoProducts())
                    Log.d("ProductInit", "Productos inicializados: ${ProductData.getAllPepsiCoProducts().size}")
                } else {
                    // TEMPORAL: Forzar actualización de productos para incluir nuevos Pepsi Black
                    Log.d("ProductInit", "Productos existentes: ${products.size}, actualizando...")
                    productRepository.insertProducts(ProductData.getAllPepsiCoProducts())
                    Log.d("ProductInit", "Base de datos actualizada con ${ProductData.getAllPepsiCoProducts().size} productos")
                    
                    // Recargar productos después de la actualización
                    val updatedProducts = productRepository.getAllProducts()
                    updatedProducts.collect { newProducts ->
                        _filteredProducts.value = newProducts
                        Log.d("ProductInit", "Lista filtrada actualizada con ${newProducts.size} productos")
                    }
                }
            }
        }
    }

    private fun updateFilteredProducts() {
        lifecycleScope.launch {
            val query = _searchQuery.value
            val category = _selectedCategory.value
            
            val products = if (query.isBlank() && category == null) {
                productRepository.getAllProducts()
            } else if (query.isNotBlank() && category != null) {
                productRepository.searchProducts(query).also {
                    // Filter by category in memory since Room doesn't support complex queries easily
                }
            } else if (query.isNotBlank()) {
                productRepository.searchProducts(query)
            } else {
                productRepository.getProductsByCategory(category!!)
            }
            
            products.collect { productList ->
                _filteredProducts.value = if (category != null && query.isNotBlank()) {
                    productList.filter { it.category == category }
                } else {
                    productList
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun navigateToScreen(screen: String) {
        _currentScreen.value = screen
    }

    // Inicializar tiendas
    private fun initializeStoreData() {
        lifecycleScope.launch {
            val existingStores = database.storeDao().getAllStores()
            existingStores.collect { stores ->
                if (stores.isEmpty()) {
                    productRepository.insertStores(ProductData.getAllStores())
                    Log.d("StoreInit", "Tiendas inicializadas: ${ProductData.getAllStores().size}")
                } else {
                    _filteredStores.value = stores
                }
            }
        }
    }

    private fun updateFilteredStores() {
        lifecycleScope.launch {
            val query = _storeSearchQuery.value
            
            val stores = if (query.isBlank()) {
                productRepository.getAllStores()
            } else {
                productRepository.searchStores(query)
            }
            
            stores.collect { storeList ->
                _filteredStores.value = storeList
            }
        }
    }

    fun updateStoreSearchQuery(query: String) {
        _storeSearchQuery.value = query
    }

    fun selectStore(store: Store?) {
        _selectedStore.value = store
    }

    // Launcher para permisos de cámara
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        _hasCameraPermission.value = isGranted
        if (!isGranted) {
            Toast.makeText(
                this,
                "Se necesita el permiso de cámara para escanear productos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar servicios
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inicializar Room Database
        database = AppDatabase.getDatabase(this)
        productRepository = ProductRepository(database.productDao(), database.cartDao(), database.purchaseDao(), database.storeDao())

        // Inicializar datos de productos y tiendas
        initializeProductData()
        initializeStoreData()

        // Observar carrito de Room
        lifecycleScope.launch {
            productRepository.getCartItems().collect { cartItems ->
                _cartItemsRoom.value = cartItems
            }
        }

        // Observar historial de compras
        lifecycleScope.launch {
            productRepository.getAllPurchases().collect { purchases ->
                _purchaseHistory.value = purchases
            }
        }

        // Observar cambios en búsqueda y filtros
        lifecycleScope.launch {
            searchQuery.collect { query ->
                updateFilteredProducts()
            }
        }

        lifecycleScope.launch {
            selectedCategory.collect { category ->
                updateFilteredProducts()
            }
        }

        // Observar tiendas
        lifecycleScope.launch {
            productRepository.getAllStores().collect { storeList ->
                _stores.value = storeList
                updateFilteredStores()
            }
        }

        // Observar cambios en búsqueda de tiendas
        lifecycleScope.launch {
            storeSearchQuery.collect { query ->
                updateFilteredStores()
            }
        }

        // Verificar permiso de cámara inicial
        checkCameraPermission()

        // Prueba de Firestore
        testFirestore()

        // Verificar y solicitar ubicación
        checkLocationPermission()

        // Configurar UI
        setContent {
            RSRTESTTheme {
                MainScreen()
            }
        }
    }

    private fun checkCameraPermission() {
        _hasCameraPermission.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                _hasCameraPermission.value = true
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Mostrar explicación adicional
                Toast.makeText(
                    this,
                    "La cámara es necesaria para detectar productos PepsiCo",
                    Toast.LENGTH_LONG
                ).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun testFirestore() {
        db.collection("orders")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshots ->
                Log.d("Firestore", "Documentos encontrados: ${snapshots.size()}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al leer orders", e)
            }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLastLocation()
        } else {
            Log.w("Location", "Permiso de ubicación denegado")
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        Log.d("Location", "Ubicación: ${it.latitude}, ${it.longitude}")
                    } ?: Log.d("Location", "Ubicación no disponible")
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error obteniendo ubicación", e)
                }
        }
    }

    private fun saveDetectionsToFirestore(
        products: List<DetectedProduct>,
        location: Location?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val batch = db.batch()

        val sessionData = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "location" to if (location != null) {
                hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                )
            } else null,
            "totalDetections" to products.size,
            "products" to products.map { product ->
                hashMapOf(
                    "name" to product.name,
                    "confidence" to product.confidence,
                    "timestamp" to product.timestamp
                )
            }
        )

        val docRef = db.collection("detection_sessions").document()
        batch.set(docRef, sessionData)

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    @Composable
    fun MainScreen() {
        // Observar estados
        val hasPermission by hasCameraPermission.collectAsState()
        val currentScreenState by currentScreen.collectAsState()
        val cartItemsRoomList by cartItemsRoom.collectAsState()
        val filteredProductsList by filteredProducts.collectAsState()
        val searchQueryState by searchQuery.collectAsState()
        val selectedCategoryState by selectedCategory.collectAsState()
        val purchaseHistoryList by purchaseHistory.collectAsState()

        // Estados locales
        var showCart by remember { mutableStateOf(false) }

        if (showCart) {
            CartScreenRoom(
                items = cartItemsRoomList,
                selectedStore = _selectedStore.collectAsState().value,
                onIncrement = { updateCartItemRoom(it, 1) },
                onDecrement = { updateCartItemRoom(it, -1) },
                onRemove = { removeFromCartRoom(it) },
                onBack = { showCart = false },
                onCheckout = { items ->
                    lifecycleScope.launch {
                        val selectedStoreValue = _selectedStore.value
                        val purchaseId = productRepository.completePurchase(items, selectedStoreValue?.customerNumber)
                        showCart = false
                        val storeInfo = selectedStoreValue?.let { " en ${it.customerName}" } ?: ""
                        Toast.makeText(this@MainActivity, "Compra completada: #${purchaseId.take(8)}$storeInfo", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            MainNavigationScreen(
                currentScreen = currentScreenState,
                hasPermission = hasPermission,
                filteredProducts = filteredProductsList,
                searchQuery = searchQueryState,
                selectedCategory = selectedCategoryState,
                purchaseHistory = purchaseHistoryList,
                cartItemCount = cartItemsRoomList.sumOf { it.quantity },
                stores = _filteredStores.collectAsState().value,
                selectedStore = _selectedStore.collectAsState().value,
                storeSearchQuery = _storeSearchQuery.collectAsState().value,
                onNavigate = { navigateToScreen(it) },
                onSearchQueryChange = { updateSearchQuery(it) },
                onCategoryChange = { updateSelectedCategory(it) },
                onRequestPermission = { requestCameraPermission() },
                onShowCart = { showCart = true },
                onAddToCart = { product ->
                    lifecycleScope.launch {
                        productRepository.addToCart(product.id)
                        Toast.makeText(this@MainActivity, "${product.name} agregado al carrito", Toast.LENGTH_SHORT).show()
                    }
                },
                onStoreSelect = { selectStore(it) },
                onStoreSearchQueryChange = { updateStoreSearchQuery(it) }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainNavigationScreen(
        currentScreen: String,
        hasPermission: Boolean,
        filteredProducts: List<Product>,
        searchQuery: String,
        selectedCategory: String?,
        purchaseHistory: List<PurchaseHistory>,
        cartItemCount: Int,
        stores: List<Store>,
        selectedStore: Store?,
        storeSearchQuery: String,
        onNavigate: (String) -> Unit,
        onSearchQueryChange: (String) -> Unit,
        onCategoryChange: (String?) -> Unit,
        onRequestPermission: () -> Unit,
        onShowCart: () -> Unit,
        onAddToCart: (Product) -> Unit,
        onStoreSelect: (Store?) -> Unit,
        onStoreSearchQueryChange: (String) -> Unit
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = PepsiColors.BackgroundDark
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner") },
                        label = { Text("Scanner") },
                        selected = currentScreen == "scanner",
                        onClick = { onNavigate("scanner") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PepsiColors.PepsiRed,
                            selectedTextColor = PepsiColors.PepsiRed,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = PepsiColors.PepsiRed.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Productos") },
                        label = { Text("Productos") },
                        selected = currentScreen == "products",
                        onClick = { onNavigate("products") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PepsiColors.PepsiRed,
                            selectedTextColor = PepsiColors.PepsiRed,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = PepsiColors.PepsiRed.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { 
                            Box {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                                if (cartItemCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(PepsiColors.PepsiRed, CircleShape)
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cartItemCount.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        label = { Text("Carrito") },
                        selected = false,
                        onClick = onShowCart,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PepsiColors.PepsiRed,
                            selectedTextColor = PepsiColors.PepsiRed,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = PepsiColors.PepsiRed.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                        label = { Text("Historial") },
                        selected = currentScreen == "history",
                        onClick = { onNavigate("history") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PepsiColors.PepsiRed,
                            selectedTextColor = PepsiColors.PepsiRed,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = PepsiColors.PepsiRed.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Store, contentDescription = "Tiendas") },
                        label = { Text("Tiendas") },
                        selected = currentScreen == "stores",
                        onClick = { onNavigate("stores") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PepsiColors.PepsiRed,
                            selectedTextColor = PepsiColors.PepsiRed,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            indicatorColor = PepsiColors.PepsiRed.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            containerColor = PepsiColors.BackgroundDark
        ) { paddingValues ->
            when (currentScreen) {
                "scanner" -> ScannerScreen(
                    hasPermission = hasPermission,
                    selectedStore = selectedStore,
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.padding(paddingValues)
                )
                "products" -> ProductsScreen(
                    products = filteredProducts,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onSearchQueryChange = onSearchQueryChange,
                    onCategoryChange = onCategoryChange,
                    onAddToCart = onAddToCart,
                    modifier = Modifier.padding(paddingValues)
                )
                "history" -> PurchaseHistoryScreen(
                    purchases = purchaseHistory,
                    selectedStore = selectedStore,
                    onStoreFilterChange = { storeId ->
                        if (storeId != null) {
                            lifecycleScope.launch {
                                productRepository.getPurchasesByStore(storeId).collect { storePurchases ->
                                    _purchaseHistory.value = storePurchases
                                }
                            }
                        } else {
                            lifecycleScope.launch {
                                productRepository.getAllPurchases().collect { allPurchases ->
                                    _purchaseHistory.value = allPurchases
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                "stores" -> StoresScreen(
                    stores = stores,
                    selectedStore = selectedStore,
                    searchQuery = storeSearchQuery,
                    onStoreSelect = onStoreSelect,
                    onSearchQueryChange = onStoreSearchQueryChange,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    @Composable
    fun ScannerScreen(
        hasPermission: Boolean,
        selectedStore: Store?,
        onRequestPermission: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        // Observar productos detectados para el scanner
        val products by detectedProducts.collectAsState()
        val currentHighlight by currentProduct.collectAsState()
        val totalDetections by detectionCount.collectAsState()
        
        // Observar estado de captura
        val isCaptureMode by isCaptureMode.collectAsState()
        val captureButtonText by captureButtonText.collectAsState()
        
        // Observar productos agregados recientemente
        val recentlyAdded by recentlyAddedProducts.collectAsState()
        
        // Observar productos faltantes
        val missingProducts by missingProducts.collectAsState()
        val currentContext by currentContext.collectAsState()

        // Para animaciones
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        // Hacer scroll automático cuando se detecta un nuevo producto
        LaunchedEffect(products.size) {
            if (products.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }

        when {
            hasPermission -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    // Cámara de fondo
                    CameraScreen()

                    // Overlay con gradiente
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )

                    // UI Principal
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header animado
                        HeaderSection(totalDetections, selectedStore)

                        // Estadísticas si hay productos
                        if (products.size >= 3) {
                            DetectionStats(products)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Producto destacado actual
                        currentHighlight?.let { product ->
                            CurrentProductCard(product)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Sección de productos faltantes
                        AnimatedVisibility(
                            visible = missingProducts.isNotEmpty(),
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            MissingProductsCard(missingProducts, currentContext)
                        }
                        
                        if (missingProducts.isNotEmpty() && products.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Lista de productos detectados
                        AnimatedVisibility(
                            visible = products.isNotEmpty(),
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            ProductHistoryCard(products, listState, recentlyAdded)
                        }
                    }

                    // Botones flotantes
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        FloatingActionButtons(
                            onSaveDetections = {
                                saveDetectionsToFirestore(
                                    products = products,
                                    location = null,
                                    onSuccess = {
                                        Toast.makeText(this@MainActivity, "¡Guardado en Firestore!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { e ->
                                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onClearDetections = {
                                _detectedProducts.value = emptyList()
                                _detectionCount.value = 0
                                _currentProduct.value = null
                                _cartItems.value = emptyList()
                                lifecycleScope.launch {
                                    productRepository.clearCart()
                                }
                            },
                            onShareDetections = {
                                shareDetections(this@MainActivity, products)
                            }
                        )
                    }
                    
                    // Botones de captura en la parte inferior
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(25.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón para resetear sesión
                            Button(
                                onClick = { resetAntiSpamSession() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray
                                ),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reset",
                                    fontSize = 12.sp
                                )
                            }
                            
                            // Botón para cambiar modo
                            Button(
                                onClick = { toggleCaptureMode() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCaptureMode) Color.Red else Color.Blue
                                ),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCaptureMode) Icons.Default.CameraAlt else Icons.Default.AutoMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCaptureMode) "Manual" else "Auto",
                                    fontSize = 14.sp
                                )
                            }
                            
                            // Botón de captura (solo visible en modo manual)
                            if (isCaptureMode) {
                                Button(
                                    onClick = { captureNow() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Green
                                    ),
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    if (captureButtonText == "⏳") {
                                        Text(
                                            text = "⏳",
                                            fontSize = 20.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Camera,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                PermissionScreen(onRequestPermission = onRequestPermission)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProductsScreen(
        products: List<Product>,
        searchQuery: String,
        selectedCategory: String?,
        onSearchQueryChange: (String) -> Unit,
        onCategoryChange: (String?) -> Unit,
        onAddToCart: (Product) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val categories = listOf(
            ProductCategory.BEBIDAS.displayName,
            ProductCategory.SNACKS.displayName,
            ProductCategory.ALIMENTOS.displayName,
            ProductCategory.CEREALES.displayName,
            ProductCategory.JUGOS.displayName,
            ProductCategory.AGUA.displayName
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PepsiColors.BackgroundDark)
                .padding(16.dp)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Buscar productos...", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = PepsiColors.PepsiBlue)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PepsiColors.PepsiBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filtros de categorías
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onCategoryChange(null) },
                        label = { Text("Todos") },
                        selected = selectedCategory == null,
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PepsiColors.PepsiRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == null,
                            borderColor = Color.White.copy(alpha = 0.5f),
                            selectedBorderColor = PepsiColors.PepsiRed
                        )
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        onClick = { 
                            onCategoryChange(if (selectedCategory == category) null else category)
                        },
                        label = { Text(category) },
                        selected = selectedCategory == category,
                        enabled = true,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PepsiColors.PepsiRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = Color.White.copy(alpha = 0.5f),
                            selectedBorderColor = PepsiColors.PepsiRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de productos
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onAddToCart = { onAddToCart(product) }
                    )
                }
            }
        }
    }

    @Composable
    fun ProductCard(
        product: Product,
        onAddToCart: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.brand,
                        color = PepsiColors.AccentOrange,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${product.category} • ${product.weight ?: "N/A"}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = product.description,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${"$%.2f".format(product.price)} ${product.currency}",
                        color = PepsiColors.SuccessGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAddToCart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PepsiColors.PepsiRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Agregar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }
    }

    @Composable
    fun PurchaseHistoryScreen(
        purchases: List<PurchaseHistory>,
        selectedStore: Store?,
        onStoreFilterChange: (String?) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PepsiColors.BackgroundDark)
                .padding(16.dp)
        ) {
            Text(
                text = "Historial de Compras",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Filtro por tienda
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Filtrar por tienda:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStoreFilterChange(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedStore == null) PepsiColors.PepsiRed else PepsiColors.CardDark
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Todas las tiendas")
                        }
                        
                        selectedStore?.let { store ->
                            Button(
                                onClick = { onStoreFilterChange(store.customerNumber) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PepsiColors.PepsiBlue
                                ),
                                modifier = Modifier.weight(2f)
                            ) {
                                Text(
                                    text = store.customerName,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (purchases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            "No hay compras registradas",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(purchases, key = { it.id }) { purchase ->
                        PurchaseHistoryCard(purchase)
                    }
                }
            }
        }
    }

    @Composable
    fun PurchaseHistoryCard(purchase: PurchaseHistory) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Compra #${purchase.id.take(8)}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(purchase.purchaseDate)),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                when (purchase.status) {
                                    "completed" -> PepsiColors.SuccessGreen
                                    "pending" -> PepsiColors.AccentOrange
                                    else -> PepsiColors.PepsiRed
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = purchase.status.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${purchase.itemCount} items",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${"$%.2f".format(purchase.totalAmount)} MXN",
                        color = PepsiColors.SuccessGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StoresScreen(
        stores: List<Store>,
        selectedStore: Store?,
        searchQuery: String,
        onStoreSelect: (Store?) -> Unit,
        onSearchQueryChange: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(PepsiColors.BackgroundDark)
                .padding(16.dp)
        ) {
            Text(
                text = "Seleccionar Tienda",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Buscar tiendas...", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = PepsiColors.PepsiBlue)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PepsiColors.PepsiBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mostrar tienda seleccionada
            selectedStore?.let { store ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PepsiColors.PepsiBlue.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tienda Seleccionada",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Button(
                                onClick = { onStoreSelect(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PepsiColors.PepsiRed
                                ),
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Deseleccionar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = store.customerName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customer #: ${store.customerNumber}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${store.chainLevel1} • ${store.chainLevel2}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Lista de tiendas
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stores, key = { it.customerNumber }) { store ->
                    StoreCard(
                        store = store,
                        isSelected = selectedStore?.customerNumber == store.customerNumber,
                        onSelect = { onStoreSelect(store) }
                    )
                }
            }
        }
    }

    @Composable
    fun StoreCard(
        store: Store,
        isSelected: Boolean,
        onSelect: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) 
                    PepsiColors.PepsiRed.copy(alpha = 0.9f) 
                else 
                    PepsiColors.CardDark.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) 
                BorderStroke(2.dp, PepsiColors.PepsiRed) 
            else null
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store.customerName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customer #: ${store.customerNumber}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = store.chainLevel1,
                        color = PepsiColors.AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (store.chainLevel2.isNotEmpty()) {
                        Text(
                            text = store.chainLevel2,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Seleccionada",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = "Tienda",
                        tint = PepsiColors.AccentOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Actualizar CartScreenRoom para agregar onCheckout
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CartScreenRoom(
        items: List<CartItemWithProduct>,
        selectedStore: Store?,
        onIncrement: (CartItemWithProduct) -> Unit,
        onDecrement: (CartItemWithProduct) -> Unit,
        onRemove: (CartItemWithProduct) -> Unit,
        onBack: () -> Unit,
        onCheckout: (List<CartItemWithProduct>) -> Unit
    ) {
        val totalPrice = items.sumOf { it.product.price * it.quantity }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Carrito de Compras", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    actions = {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "${items.sumOf { it.quantity }} items",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            selectedStore?.let { store ->
                                Text(
                                    text = store.customerName.take(20) + if (store.customerName.length > 20) "..." else "",
                                    color = PepsiColors.AccentOrange,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PepsiColors.PepsiBlue)
                )
            },
            containerColor = PepsiColors.BackgroundDark,
            bottomBar = {
                if (items.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PepsiColors.PepsiBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total:",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${"%.2f".format(totalPrice)} MXN",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onCheckout(items) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PepsiColors.SuccessGreen)
                            ) {
                                Text("Proceder al Pago", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            "No hay productos en el carrito",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.product.id }) { item ->
                        CartItemRowRoom(item, onIncrement, onDecrement, onRemove)
                    }
                }
            }
        }
    }

    @Composable
    fun CartItemRowRoom(
        item: CartItemWithProduct,
        onIncrement: (CartItemWithProduct) -> Unit,
        onDecrement: (CartItemWithProduct) -> Unit,
        onRemove: (CartItemWithProduct) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.CardDark.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.product.brand,
                            color = PepsiColors.AccentOrange,
                            fontSize = 12.sp
                        )
                        Text(
                            text = item.product.category + " • " + (item.product.weight ?: ""),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${"$%.2f".format(item.product.price)} MXN",
                            color = PepsiColors.SuccessGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    IconButton(onClick = { onRemove(item) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = PepsiColors.PepsiRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtotal: ${"$%.2f".format(item.product.price * item.quantity)} MXN",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                PepsiColors.PepsiBlue.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = { onDecrement(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Disminuir",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = item.quantity.toString(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { onIncrement(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Aumentar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HeaderSection(detectionCount: Int, selectedStore: Store? = null) {
        val infiniteTransition = rememberInfiniteTransition()
        val animatedColor by infiniteTransition.animateColor(
            initialValue = PepsiColors.PepsiBlue,
            targetValue = PepsiColors.LightBlue,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.BackgroundDark.copy(alpha = 0.9f)
            ),
            border = BorderStroke(2.dp, animatedColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PepsiCo Scanner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    selectedStore?.let { store ->
                        Text(
                            text = "📍 ${store.customerName.take(25)}${if (store.customerName.length > 25) "..." else ""}",
                            fontSize = 12.sp,
                            color = PepsiColors.AccentOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Detectando productos...",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(animatedColor)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = detectionCount.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    fun DetectionStats(products: List<DetectedProduct>) {
        val productCounts = products.groupBy { it.name }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        if (productCounts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Productos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = PepsiColors.AccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    productCounts.forEach { (product, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = product,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        PepsiColors.AccentOrange,
                                        CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = count.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CurrentProductCard(product: DetectedProduct) {
        val animatedProgress = remember { Animatable(0f) }

        LaunchedEffect(product) {
            animatedProgress.animateTo(
                targetValue = product.confidence,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.CardDark
            ),
            border = BorderStroke(
                2.dp,
                Brush.linearGradient(
                    colors = listOf(
                        PepsiColors.PepsiRed,
                        PepsiColors.PepsiBlue
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rotation by rememberInfiniteTransition().animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing)
                    )
                )

                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = PepsiColors.AccentOrange
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = product.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.value)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        PepsiColors.SuccessGreen,
                                        PepsiColors.AccentOrange
                                    )
                                )
                            )
                    )

                    Text(
                        text = "${(product.confidence * 100).toInt()}%",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(product.timestamp)),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }

    @Composable
    fun MissingProductsCard(
        missingProducts: List<MissingProduct>,
        context: ShelfContext
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 PepsiCo Faltantes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = context.displayName,
                        fontSize = 12.sp,
                        color = PepsiColors.LightBlue,
                        modifier = Modifier
                            .background(
                                PepsiColors.LightBlue.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(missingProducts) { missing ->
                        MissingProductItem(missing)
                    }
                }
            }
        }
    }

    @Composable
    fun MissingProductItem(missing: MissingProduct) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red.copy(alpha = 0.2f),
                            Color(0xFFFF8C00).copy(alpha = 0.1f) // Orange color
                        )
                    )
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFFF8C00), // Orange color
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = missing.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = missing.reason,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(missing.confidence * 100).toInt()}%",
                    color = Color(0xFFFF8C00), // Orange color
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = when {
                        missing.confidence >= 0.7f -> Icons.Default.ErrorOutline
                        missing.confidence >= 0.5f -> Icons.Default.Warning
                        else -> Icons.Default.Help
                    },
                    contentDescription = null,
                    tint = when {
                        missing.confidence >= 0.7f -> Color.Red
                        missing.confidence >= 0.5f -> Color(0xFFFF8C00) // Orange color
                        else -> Color.Yellow
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    @Composable
    fun ProductHistoryCard(
        products: List<DetectedProduct>,
        listState: LazyListState,
        recentlyAdded: Set<String>
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.BackgroundDark.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial de Detecciones",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = PepsiColors.LightBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = products.take(10),
                        key = { it.id }
                    ) { product ->
                        ProductHistoryItem(product, recentlyAdded.contains(product.name))
                    }
                }
            }
        }
    }

    @Composable
    fun ProductHistoryItem(product: DetectedProduct, isRecentlyAdded: Boolean = false) {
        val animatedAlpha = remember { Animatable(0f) }

        LaunchedEffect(product) {
            animatedAlpha.animateTo(1f, animationSpec = tween(500))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = animatedAlpha.value }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PepsiColors.PepsiBlue.copy(alpha = 0.3f),
                            PepsiColors.PepsiBlue.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PepsiColors.AccentOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de agregado recientemente
                if (isRecentlyAdded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Agregado recientemente",
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Text(
                    text = "${(product.confidence * 100).toInt()}%",
                    color = PepsiColors.SuccessGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(product.timestamp)),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    fun FloatingActionButtons(
        onSaveDetections: () -> Unit,
        onClearDetections: () -> Unit,
        onShareDetections: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = onShareDetections,
                containerColor = PepsiColors.LightBlue,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = onSaveDetections,
                containerColor = PepsiColors.SuccessGreen,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Guardar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = onClearDetections,
                containerColor = PepsiColors.PepsiRed,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Limpiar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    @Composable
    fun PermissionScreen(onRequestPermission: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PepsiColors.DarkBlue,
                            PepsiColors.BackgroundDark
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PepsiColors.CardDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = PepsiColors.PepsiRed
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Permiso de Cámara",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Para detectar productos PepsiCo necesitamos acceso a tu cámara",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PepsiColors.PepsiRed
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permitir Acceso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CameraScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(context) }

        DisposableEffect(previewView) {
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Log.e("CameraX", "Error vinculando casos de uso", e)
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose { }
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }

    @androidx.camera.core.ExperimentalGetImage
    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        override fun analyze(imageProxy: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            // Analizar solo cada 3000ms para evitar spam DRÁSTICO
            if (currentTimestamp - lastAnalyzedTimestamp >= 3000) {
                
                lifecycleScope.launch {
                    try {
                        // MODO DEBUG: Usar detecciones simuladas para probar la lógica
                        Log.d("MainActivity", "Iniciando detección...")
                        
                        // Usar detección real con formato corregido
                        val yoloDetections = yoloDetector.detectObjects(imageProxy)
                        
                        // DEBUG: mostrar si hay detecciones
                        Log.d("MainActivity", "Detecciones obtenidas: ${yoloDetections.size}")
                        val pepsicoDetections = mutableListOf<DetectedProduct>()
                        
                        Log.d("MainActivity", "Procesando ${yoloDetections.size} detecciones...")
                        
                        // LIMITAR: Solo procesar las primeras 3 detecciones para evitar spam
                        val limitedDetections = yoloDetections.take(3)
                        Log.d("MainActivity", "Limitando a ${limitedDetections.size} detecciones para evitar spam")
                        
                        // DEDUPLICACIÓN: Solo agregar productos únicos
                        val uniqueDetections = mutableSetOf<String>()
                        val currentProducts = _detectedProducts.value.map { it.name.lowercase() }
                        
                        for (detection in limitedDetections) {
                            val className = detection.className.lowercase()
                            
                            // Solo procesar si no hemos visto este producto recientemente
                            if (!currentProducts.contains(className) && !uniqueDetections.contains(className)) {
                                Log.d("MainActivity", "🔄 Procesando detección NUEVA: $className con confianza ${detection.confidence}")
                                Log.d("MainActivity", "🔄 Modo actual: ${if (_isCaptureMode.value) "MANUAL" else "AUTOMÁTICO"}")
                                
                                uniqueDetections.add(className)
                                
                                // SIMPLIFICADO: Crear detección directa sin buscar en BD primero
                                val detectedProduct = DetectedProduct(
                                    name = detection.className,
                                    confidence = detection.confidence
                                )
                                pepsicoDetections.add(detectedProduct)
                                
                                // Actualizar producto actual
                                if (detection.confidence > 0.3f) {
                                    _currentProduct.value = detectedProduct
                                    Log.d("MainActivity", "Producto actual actualizado: ${detection.className}")
                                }
                                
                                // Solo agregar automáticamente si NO está en modo captura manual
                                Log.d("MainActivity", "🔄 ¿Agregar automáticamente? Modo manual: ${_isCaptureMode.value}")
                                if (!_isCaptureMode.value) {
                                // Buscar producto en la base de datos Room para agregarlo al carrito
                                Log.d("MainActivity", "🔍 Buscando en BD: '$className' -> '${className.lowercase()}'")
                                val foundProduct = productRepository.findProductByDetectionKeyword(className.lowercase())
                                Log.d("MainActivity", "🔍 Resultado búsqueda: ${foundProduct?.name ?: "NO ENCONTRADO"}")
                                foundProduct?.let { product ->
                                    Log.d("MainActivity", "Producto encontrado en BD: '${product.name}', verificando cooldown...")
                                    // Verificar cooldown antes de agregar
                                    if (canAddProduct(product.name)) {
                                        // Agregar directamente al carrito usando el producto encontrado
                                        addProductToCart(product)
                                        markProductAsAdded(product.name)
                                        Log.d("MainActivity", "✅ Producto agregado automáticamente: ${product.name}")
                                        runOnUiThread {
                                            Toast.makeText(this@MainActivity, "✅ ${product.name} agregado", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (productsAddedInSession.contains(product.name)) {
                                            Log.d("MainActivity", "🚫 Producto YA AGREGADO en esta sesión: ${product.name}")
                                        } else {
                                            Log.d("MainActivity", "❌ Producto en cooldown: ${product.name}")
                                        }
                                    }
                                } ?: run {
                                    Log.d("MainActivity", "Producto no encontrado en BD: $className")
                                }
                            } else {
                                Log.d("MainActivity", "Modo captura manual - producto detectado pero no agregado: $className")
                            }
                                
                                Log.d("MainActivity", "Detectado: ${detection.className} (${detection.confidence})")
                            } else {
                                Log.d("MainActivity", "⏭️ Saltando detección duplicada: $className")
                            }
                        }
                        
                        // Actualizar la lista de productos detectados
                        Log.d("MainActivity", "Detecciones PepsiCo encontradas: ${pepsicoDetections.size}")
                        
                        if (pepsicoDetections.isNotEmpty()) {
                            val currentTime = System.currentTimeMillis()
                            
                            // Solo actualizar la lista cada 2 segundos para evitar spam visual
                            if (currentTime - lastDetectionListUpdate >= DETECTION_LIST_THROTTLE_MS) {
                                Log.d("MainActivity", "✅ Agregando ${pepsicoDetections.size} detecciones a la lista (throttled)")
                                
                                val currentList = _detectedProducts.value.toMutableList()
                                currentList.addAll(0, pepsicoDetections)
                                _detectedProducts.value = currentList.take(50) // Mantener máximo 50
                                _detectionCount.value += pepsicoDetections.size
                                lastDetectionListUpdate = currentTime
                                
                                Log.d("MainActivity", "Lista actualizada. Total detecciones: ${_detectedProducts.value.size}")
                                Log.d("MainActivity", "Contador actualizado: ${_detectionCount.value}")
                            } else {
                                val remainingTime = DETECTION_LIST_THROTTLE_MS - (currentTime - lastDetectionListUpdate)
                                Log.d("MainActivity", "⏳ Lista en throttle, faltan ${remainingTime}ms")
                            }
                            
                            // 🔍 ANÁLISIS DE PRODUCTOS PEPSICO FALTANTES
                            val missingProducts = analyzeMissingProducts(pepsicoDetections)
                            _missingProducts.value = missingProducts
                            Log.d("MainActivity", "🔍 Productos PepsiCo faltantes analizados: ${missingProducts.size}")
                            Log.d("MainActivity", "🔍 Contexto detectado: ${_currentContext.value.displayName}")
                            missingProducts.forEach { missing ->
                                Log.d("MainActivity", "🔍 Falta (PepsiCo): ${missing.name} (${(missing.confidence*100).toInt()}%) - ${missing.reason}")
                            }
                            
                            // Vibrar al detectar
                            vibrateOnDetection(this@MainActivity)
                        } else {
                            Log.d("MainActivity", "No hay detecciones para agregar")
                        }
                        
                    } catch (e: Exception) {
                        Log.e("YOLO", "Error en detección YOLO: ${e.message}", e)
                    } finally {
                        imageProxy.close()
                        lastAnalyzedTimestamp = currentTimestamp
                    }
                }
            } else {
                imageProxy.close()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        yoloDetector.close()
    }
}
