package com.example.rsrtest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.rsrtest.data.*
import com.example.rsrtest.ml.CameraMLManager
import com.example.rsrtest.navigation.AppNavigation
import com.example.rsrtest.ui.components.*
import com.example.rsrtest.ui.theme.RSRTESTTheme
import com.example.rsrtest.viewmodel.DetectionViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Repository and ML Manager
    private lateinit var productRepository: ProductRepository
    private lateinit var cameraMLManager: CameraMLManager
    private lateinit var detectionViewModel: DetectionViewModel

    // Permission Management
    private val _hasCameraPermission = MutableStateFlow(false)
    private val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    // Store Management
    private val _selectedStore = MutableStateFlow<Store?>(null)
    private val selectedStore: StateFlow<Store?> = _selectedStore.asStateFlow()

    // Products and Search
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    private val products: StateFlow<List<Product>> = _products.asStateFlow()
    
    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    private val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    private val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Cart Management
    private val _cartItems = MutableStateFlow<List<CartItemWithProduct>>(emptyList())
    private val cartItems: StateFlow<List<CartItemWithProduct>> = _cartItems.asStateFlow()

    // Purchase History
    private val _purchaseHistory = MutableStateFlow<List<PurchaseHistory>>(emptyList())
    private val purchaseHistory: StateFlow<List<PurchaseHistory>> = _purchaseHistory.asStateFlow()

    // Stores
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    private val stores: StateFlow<List<Store>> = _stores.asStateFlow()
    
    private val _storeSearchQuery = MutableStateFlow("")
    private val storeSearchQuery: StateFlow<String> = _storeSearchQuery.asStateFlow()

    // Capture mode states
    private val _captureButtonText = MutableStateFlow("📷")
    private val captureButtonText: StateFlow<String> = _captureButtonText.asStateFlow()
    
    private val _recentlyAddedProducts = MutableStateFlow<Set<String>>(emptySet())
    private val recentlyAddedProducts: StateFlow<Set<String>> = _recentlyAddedProducts.asStateFlow()
    
    private val _missingProducts = MutableStateFlow<List<com.example.rsrtest.data.MissingProduct>>(emptyList())
    private val missingProducts: StateFlow<List<com.example.rsrtest.data.MissingProduct>> = _missingProducts.asStateFlow()
    
    private val _currentContext = MutableStateFlow(com.example.rsrtest.data.ShelfContext.UNKNOWN)
    private val currentContext: StateFlow<com.example.rsrtest.data.ShelfContext> = _currentContext.asStateFlow()

    // Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        _hasCameraPermission.value = isGranted
        if (isGranted) {
            Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize components
        initializeComponents()
        
        // Check initial camera permission
        checkCameraPermission()
        
        // Re-check permissions when app resumes
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                checkCameraPermission()
            }
        }

        // Setup UI with new navigation system
        setContent {
            RSRTESTTheme {
                val navController = rememberNavController()
                
                AppNavigation(
                    navController = navController,
                    // Scanner Screen Dependencies
                    hasPermission = hasCameraPermission.collectAsState().value,
                    selectedStore = selectedStore.collectAsState().value,
                    detectedProducts = detectionViewModel.pepsicoDetections,
                    currentProduct = detectionViewModel.currentProduct,
                    detectionCount = detectionViewModel.detectionCount,
                    isCaptureMode = detectionViewModel.isCaptureMode,
                    captureButtonText = captureButtonText,
                    recentlyAddedProducts = recentlyAddedProducts,
                    missingProducts = missingProducts,
                    currentContext = currentContext,
                    onRequestPermission = { requestCameraPermission() },
                    onSaveDetections = { detections -> 
                        // TODO: Implement save to Firestore
                        Toast.makeText(this@MainActivity, "Detecciones guardadas", Toast.LENGTH_SHORT).show()
                    },
                    onClearDetections = { 
                        detectionViewModel.clearDetections()
                        lifecycleScope.launch {
                            productRepository.clearCart()
                        }
                    },
                    onShareDetections = { detections ->
                        // TODO: Implement share functionality
                        Toast.makeText(this@MainActivity, "Compartir detecciones", Toast.LENGTH_SHORT).show()
                    },
                    onResetAntiSpamSession = { 
                        detectionViewModel.clearDetections()
                        Toast.makeText(this@MainActivity, "Detecciones reseteadas", Toast.LENGTH_SHORT).show()
                    },
                    onToggleCaptureMode = { 
                        detectionViewModel.toggleCaptureMode()
                    },
                    onCaptureNow = { 
                        detectionViewModel.addCurrentProductToCart()
                    },
                    
                    // Products Screen Dependencies
                    products = filteredProducts.collectAsState().value,
                    productSearchQuery = searchQuery.collectAsState().value,
                    selectedCategory = selectedCategory.collectAsState().value,
                    onProductSearchQueryChange = { updateSearchQuery(it) },
                    onCategoryChange = { updateSelectedCategory(it) },
                    onAddToCart = { product ->
                        lifecycleScope.launch {
                            productRepository.addToCart(product.id)
                            Toast.makeText(this@MainActivity, "${product.name} agregado al carrito", Toast.LENGTH_SHORT).show()
                        }
                    },
                    
                    // Cart Screen Dependencies
                    cartItems = cartItems.collectAsState().value,
                    onUpdateCartQuantity = { productId, newQuantity ->
                        lifecycleScope.launch {
                            productRepository.updateCartItemQuantity(productId, newQuantity)
                        }
                    },
                    onRemoveFromCart = { productId ->
                        lifecycleScope.launch {
                            productRepository.removeFromCart(productId)
                        }
                    },
                    onCheckout = {
                        lifecycleScope.launch {
                            try {
                                val items = cartItems.value
                                val selectedStoreValue = selectedStore.value
                                val purchaseId = productRepository.completePurchase(items, selectedStoreValue?.customerNumber)
                                val storeInfo = selectedStoreValue?.let { " en ${it.customerName}" } ?: ""
                                Toast.makeText(this@MainActivity, "Compra completada: #${purchaseId.take(8)}$storeInfo", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Error en checkout: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    
                    // History Screen Dependencies
                    purchaseHistory = purchaseHistory.collectAsState().value,
                    onStoreFilterChange = { storeId ->
                        // TODO: Implement store filter
                    },
                    
                    // Stores Screen Dependencies
                    stores = stores.collectAsState().value,
                    storeSearchQuery = storeSearchQuery.collectAsState().value,
                    onStoreSelect = { store -> 
                        _selectedStore.value = store
                    },
                    onStoreSearchQueryChange = { query ->
                        _storeSearchQuery.value = query
                    },
                    
                    // Composable Providers
                    cameraContent = { 
                        CameraPreview(
                            cameraMLManager = cameraMLManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    headerSection = { count, store -> 
                        HeaderSection(count, store)
                    },
                    detectionStats = { detections -> 
                        DetectionStats(detections)
                    },
                    currentProductCard = { product -> 
                        CurrentProductCard(product)
                    },
                    missingProductsCard = { missing, context ->
                        // TODO: Implement missing products card
                        androidx.compose.foundation.layout.Box {}
                    },
                    productHistoryCard = { detections, listState, recentlyAdded ->
                        // TODO: Implement product history card
                        androidx.compose.foundation.layout.Box {}
                    },
                    floatingActionButtons = { onSave, onClear, onShare ->
                        // TODO: Implement floating action buttons
                        androidx.compose.foundation.layout.Box {}
                    },
                    permissionScreen = { onRequest ->
                        com.example.rsrtest.ui.screens.PermissionScreen(
                            onRequestPermission = onRequest
                        )
                    },
                    productCard = { product, onAdd ->
                        // TODO: Implement product card
                        androidx.compose.foundation.layout.Box {}
                    },
                    cartItemRow = { item, onUpdate, onRemove ->
                        // TODO: Implement cart item row
                        androidx.compose.foundation.layout.Box {}
                    },
                    purchaseHistoryCard = { purchase ->
                        // TODO: Implement purchase history card
                        androidx.compose.foundation.layout.Box {}
                    },
                    storeCard = { store, selectedStore, onSelect ->
                        // TODO: Implement store card
                        androidx.compose.foundation.layout.Box {}
                    }
                )
            }
        }
    }

    private fun initializeComponents() {
        // Initialize database and repository
        val database = AppDatabase.getDatabase(this)
        productRepository = ProductRepository(
            database.productDao(),
            database.cartDao(),
            database.purchaseDao(),
            database.storeDao()
        )
        
        // Initialize detection ViewModel
        detectionViewModel = DetectionViewModel(productRepository)
        
        // Initialize camera ML manager
        cameraMLManager = CameraMLManager(this) { detections ->
            detections.forEach { detection ->
                detectionViewModel.processDetection(detection.name, detection.confidence)
            }
        }
        
        // Setup data observers
        setupDataObservers()
    }

    private fun setupDataObservers() {
        // Observe products from repository
        lifecycleScope.launch {
            productRepository.getAllProducts().collect { productList ->
                _products.value = productList
                updateFilteredProducts()
            }
        }

        // Observe cart items
        lifecycleScope.launch {
            productRepository.getCartItems().collect { items ->
                _cartItems.value = items
            }
        }

        // Observe purchase history
        lifecycleScope.launch {
            productRepository.getAllPurchases().collect { purchases ->
                _purchaseHistory.value = purchases
            }
        }

        // Observe stores
        lifecycleScope.launch {
            productRepository.getAllStores().collect { storeList ->
                _stores.value = storeList
            }
        }

        // Observe search and category changes
        lifecycleScope.launch {
            searchQuery.collect { 
                updateFilteredProducts()
            }
        }

        lifecycleScope.launch {
            selectedCategory.collect { 
                updateFilteredProducts()
            }
        }
    }

    private fun updateFilteredProducts() {
        val query = _searchQuery.value
        val category = _selectedCategory.value
        val allProducts = _products.value

        _filteredProducts.value = allProducts.filter { product ->
            val matchesSearch = if (query.isBlank()) true else 
                product.name.contains(query, ignoreCase = true) ||
                product.brand.contains(query, ignoreCase = true) ||
                product.keywords.contains(query, ignoreCase = true)

            val matchesCategory = if (category == null) true else 
                product.category == category

            matchesSearch && matchesCategory
        }
    }

    private fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    private fun checkCameraPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        _hasCameraPermission.value = hasPermission
        
        // Log para debugging
        android.util.Log.d("CameraPermission", "Permission status: $hasPermission")
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraMLManager.shutdown()
    }
}