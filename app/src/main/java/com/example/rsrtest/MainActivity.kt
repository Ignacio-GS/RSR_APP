package com.example.rsrtest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.room.Room
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import com.example.rsrtest.core.permission.PermissionManager
import com.example.rsrtest.core.ui.UIStateHolder
import com.example.rsrtest.core.navigation.AppNavigationHandler
import com.example.rsrtest.core.error.ErrorHandler
import com.example.rsrtest.core.network.ConnectivityManager
import com.example.rsrtest.core.offline.OfflineManager
import com.example.rsrtest.data.*
import com.example.rsrtest.ml.CameraMLManager
import com.example.rsrtest.navigation.AppNavigation
import com.example.rsrtest.ui.theme.RSRTESTTheme
import com.example.rsrtest.viewmodel.DetectionViewModel
import com.example.rsrtest.ui.components.*
import com.example.rsrtest.ui.screens.PermissionScreen
// dagger.hilt.android.AndroidEntryPoint - temporalmente deshabilitado
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
// import javax.inject.Inject - temporalmente deshabilitado

/**
 * MainActivity refactorizada con Clean Architecture y Hilt
 * 
 * Esta versión implementa:
 * - Inyección de dependencias con Hilt
 * - Separación clara de responsabilidades
 * - Manejo de estado centralizado
 * - Navegación delegada
 * - Permisos gestionados por PermissionManager
 * 
 * Responsabilidades reducidas:
 * - Ciclo de vida de la Activity
 * - Configuración inicial de componentes
 * - Delegación en manejadores especializados
 */
class MainActivity : ComponentActivity() {
    
    // Dependencias (creadas manualmente temporalmente)
    private lateinit var permissionManager: PermissionManager
    private lateinit var uiStateHolder: UIStateHolder
    private lateinit var navigationHandler: AppNavigationHandler
    private lateinit var cameraMLManager: CameraMLManager
    private lateinit var errorHandler: ErrorHandler
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var offlineManager: OfflineManager
    
    // ViewModel creado manualmente temporalmente
    private val detectionViewModel: DetectionViewModel by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetectionViewModel(uiStateHolder.productRepository) as T
                }
            }
        )[DetectionViewModel::class.java]
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicialización de dependencias manualmente
        initializeDependencies()
        
        // Inicialización de componentes
        initializeComponents()
        
        // Configurar UI
        setupUI()
    }
    
    /**
     * Inicializa las dependencias manualmente (temporal)
     */
    private fun initializeDependencies() {
        // Crear dependencias de infraestructura
        errorHandler = ErrorHandler(this)
        connectivityManager = ConnectivityManager(this)
        permissionManager = PermissionManager(this)
        navigationHandler = AppNavigationHandler()
        
        // Crear repositorio y UIStateHolder
        val database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "rsr_database"
        ).build()
        
        val productDao = database.productDao()
        val cartDao = database.cartDao()
        val purchaseDao = database.purchaseDao()
        val storeDao = database.storeDao()
        
        val productRepository = ProductRepository(
            productDao = productDao,
            cartDao = cartDao,
            purchaseDao = purchaseDao,
            storeDao = storeDao,
            errorHandler = errorHandler
        )
        
        uiStateHolder = UIStateHolder(productRepository)
        offlineManager = OfflineManager(this, connectivityManager, errorHandler, productRepository)
        
        cameraMLManager = CameraMLManager(
            context = this,
            onDetection = { detections ->
                detections.forEach { detection ->
                    detectionViewModel.processDetection(detection.name, detection.confidence)
                }
            },
            errorHandler = errorHandler,
            offlineManager = offlineManager
        )
    }
    
    /**
     * Inicializa los componentes necesarios para la aplicación
     */
    private fun initializeComponents() {
        // Configurar permisos
        setupPermissionManager()
        
        // Configurar navegación
        setupNavigation()
        
        // Configurar observadores de estado
        setupStateObservers()
        
        // Configurar manejador de ML
        setupMLManager()
    }
    
    /**
     * Configura el PermissionManager y sus launchers
     */
    private fun setupPermissionManager() {
        // Inicializar los launchers de permisos
        val cameraPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionManager.handleCameraPermissionResult(isGranted)
        }

        val locationPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionManager.handleLocationPermissionResult(isGranted)
        }

        permissionManager.initializeLaunchers(
            cameraLauncher = { permission -> cameraPermissionLauncher.launch(permission) },
            locationLauncher = { permission -> locationPermissionLauncher.launch(permission) }
        )

        // Verificar permisos iniciales
        permissionManager.checkPermissions()
    }
    
    /**
     * Configura el sistema de navegación
     */
    private fun setupNavigation() {
        // La navegación se configurará cuando se cree el NavController
        // en el composable principal
    }
    
    /**
     * Configura los observadores de conectividad y errores
     */
    private fun setupConnectivityAndErrorObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar estado de conectividad
                launch {
                    connectivityManager.isConnected.collect { isConnected ->
                        // Actualizar UI según estado de conexión
                        if (isConnected) {
                            // Intentar sincronizar cuando se recupera la conexión
                            offlineManager.synchronizePendingItems()
                        }
                    }
                }
                
                // Observar errores globales
                launch {
                    errorHandler.errors.collect { errors ->
                        // Mostrar errores en UI o manejarlos
                        if (errors.isNotEmpty()) {
                            val latestError = errors.last()
                            // Aquí podrías mostrar un snackbar o notificación
                            Log.d("MainActivity", "Error observado: ${latestError.message}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Configura los observadores de estado reactivos
     */
    private fun setupStateObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar productos
                launch {
                    // Aquí se conectarán los flujos de datos al UIStateHolder
                    // Por ahora, mantenemos la lógica existente
                }
                
                // Observar carrito
                launch {
                    // Lógica de carrito existente
                }
                
                // Observar tiendas
                launch {
                    // Lógica de tiendas existente
                }
                
                // Observar historial de compras
                launch {
                    // Lógica de historial existente
                }
                
                // Observar estado offline
                launch {
                    offlineManager.isOfflineMode.collect { isOffline ->
                        // Actualizar UI según modo offline
                        Log.d("MainActivity", "Modo offline: $isOffline")
                    }
                }
            }
        }
    }
    
    /**
     * Configura el manejador de Machine Learning
     */
    private fun setupMLManager() {
        // El cameraMLManager ya se inicializó en initializeDependencies()
        // No es necesario volver a crearlo aquí
    }
    
    /**
     * Configura la interfaz de usuario principal
     */
    private fun setupUI() {
        setContent {
            RSRTESTTheme {
                val navController = rememberNavController()
                
                // Configurar el NavController en el navigation handler
                LaunchedEffect(navController) {
                    navigationHandler.setNavController(navController)
                }
                
                // Aquí va la navegación principal conectada a la nueva arquitectura
                SetupAppNavigation(
                    uiStateHolder = uiStateHolder,
                    detectionViewModel = detectionViewModel,
                    permissionManager = permissionManager,
                    onRequestPermission = {
                        permissionManager.requestCameraPermission()
                    },
                    onSaveDetections = { detections ->
                        // Guardar detecciones en Firestore o base de datos local
                        Log.d("MainActivity", "Guardando ${detections.size} detecciones")
                    },
                    onClearDetections = { detectionViewModel.clearDetections() },
                    onShareDetections = { detections ->
                        // Compartir detecciones
                        Log.d("MainActivity", "Compartiendo ${detections.size} detecciones")
                    },
                    onResetAntiSpamSession = {
                        detectionViewModel.clearDetections()
                        // TODO: Implement resetAntiSpamSession and clearRecentlyAdded
                    },
                    onToggleCaptureMode = { detectionViewModel.toggleCaptureMode() },
                    onCaptureNow = { detectionViewModel.addCurrentProductToCart() },
                    onUpdateCartQuantity = { productId, quantity ->
                        // TODO: Implement updateCartItemQuantity
                    },
                    onRemoveFromCart = { productId ->
                        // TODO: Implement removeFromCart
                    },
                    onCheckout = {
                        Log.d("MainActivity", "Iniciando checkout")
                    },
                    onStoreFilterChange = { storeId ->
                        // TODO: Implement updateStoreFilter
                    },
                    onStoreSelect = { store ->
                        uiStateHolder.selectStore(store ?: Store("", "", "", ""))
                    },
                    cameraContent = {
                        CameraPreview(cameraMLManager = cameraMLManager)
                    },
                    headerSection = { count, store ->
                        HeaderSection(detectionCount = count, selectedStore = store)
                    },
                    detectionStats = { detections ->
                        DetectionStats(products = detections)
                    },
                    currentProductCard = { product ->
                        CurrentProductCard(product = product)
                    },
                    missingProductsCard = { missing, context ->
                        MissingProductsCard(missingProducts = missing, currentContext = context)
                    },
                    productHistoryCard = { products, state, added ->
                        ProductHistoryCard(
                            products = products,
                            listState = state,
                            recentlyAdded = added
                        )
                    },
                    floatingActionButtons = { onSave, onClear, onShare ->
                        FloatingActionButtons(
                            onSave = onSave,
                            onClear = onClear,
                            onShare = onShare
                        )
                    },
                    permissionScreen = { onRequest ->
                        PermissionScreen(onRequestPermission = onRequest)
                    },
                    productCard = { product, onAdd ->
                        ProductCard(
                            product = product,
                            onAddToCart = { onAdd(product) }
                        )
                    },
                    cartItemRow = { item, onUpdate, onRemove ->
                        CartItemRow(
                            cartItem = item,
                            onUpdateQuantity = { quantity -> onUpdate(item.product.id, quantity) },
                            onRemove = { onRemove(item.product.id) }
                        )
                    },
                    purchaseHistoryCard = { purchase ->
                        PurchaseHistoryCard(
                            purchase = purchase,
                            onPurchaseClick = { /* TODO: Handle purchase click */ }
                        )
                    },
                    storeCard = { store, selected, onSelect ->
                        StoreCard(
                            store = store,
                            isSelected = selected?.customerNumber == store.customerNumber,
                            onSelect = { onSelect(store) }
                        )
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Limpiar recursos
        cameraMLManager.shutdown()
        connectivityManager.destroy()
        // El ErrorHandler y OfflineManager no necesitan limpieza explícita
    }
}

/**
 * Composable principal para la navegación de la aplicación
 * 
 * Ahora conecta la UI existente con la nueva arquitectura
 * utilizando UIStateHolder y el sistema de navegación existente
 */
@Composable
fun SetupAppNavigation(
    uiStateHolder: UIStateHolder,
    detectionViewModel: DetectionViewModel,
    permissionManager: PermissionManager,
    onRequestPermission: () -> Unit,
    onSaveDetections: (List<DetectedProduct>) -> Unit,
    onClearDetections: () -> Unit,
    onShareDetections: (List<DetectedProduct>) -> Unit,
    onResetAntiSpamSession: () -> Unit,
    onToggleCaptureMode: () -> Unit,
    onCaptureNow: () -> Unit,
    onUpdateCartQuantity: (String, Int) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onCheckout: () -> Unit,
    onStoreFilterChange: (String?) -> Unit,
    onStoreSelect: (Store?) -> Unit,
    cameraContent: @Composable () -> Unit,
    headerSection: @Composable (Int, Store?) -> Unit,
    detectionStats: @Composable (List<DetectedProduct>) -> Unit,
    currentProductCard: @Composable (DetectedProduct) -> Unit,
    missingProductsCard: @Composable (List<MissingProduct>, ShelfContext) -> Unit,
    productHistoryCard: @Composable (List<DetectedProduct>, androidx.compose.foundation.lazy.LazyListState, Set<String>) -> Unit,
    floatingActionButtons: @Composable (onSave: () -> Unit, onClear: () -> Unit, onShare: () -> Unit) -> Unit,
    permissionScreen: @Composable (() -> Unit) -> Unit,
    productCard: @Composable (Product, (Product) -> Unit) -> Unit,
    cartItemRow: @Composable (CartItemWithProduct, (String, Int) -> Unit, (String) -> Unit) -> Unit,
    purchaseHistoryCard: @Composable (PurchaseHistory) -> Unit,
    storeCard: @Composable (Store, Store?, (Store?) -> Unit) -> Unit
) {
    // Collect state from ViewModels and Holders
    val hasPermission by permissionManager.hasCameraPermission.collectAsState()
    val selectedStore by uiStateHolder.selectedStore.collectAsState()
    val detectedProducts by detectionViewModel.pepsicoDetections.collectAsState()
    val currentProduct by detectionViewModel.currentProduct.collectAsState()
    val detectionCount by detectionViewModel.detectionCount.collectAsState()
    val isCaptureMode by detectionViewModel.isCaptureMode.collectAsState()
    val captureButtonText by uiStateHolder.captureButtonText.collectAsState()
    val recentlyAddedProducts by uiStateHolder.recentlyAddedProducts.collectAsState()
    val missingProducts by uiStateHolder.missingProducts.collectAsState()
    val currentContext by uiStateHolder.currentContext.collectAsState()
    
    val products by uiStateHolder.filteredProducts.collectAsState()
    val productSearchQuery by uiStateHolder.searchQuery.collectAsState()
    val selectedCategory by uiStateHolder.selectedCategory.collectAsState()
    
    val cartItems by uiStateHolder.cartItems.collectAsState()
    val purchaseHistory by uiStateHolder.purchaseHistory.collectAsState()
    
    val stores by uiStateHolder.stores.collectAsState()
    val storeSearchQuery by uiStateHolder.storeSearchQuery.collectAsState()
    
    // Create StateFlow wrappers for collected values
    val detectedProductsFlow = remember { MutableStateFlow(detectedProducts) }
    val currentProductFlow = remember { MutableStateFlow(currentProduct) }
    val detectionCountFlow = remember { MutableStateFlow(detectionCount) }
    val isCaptureModeFlow = remember { MutableStateFlow(isCaptureMode) }
    val captureButtonTextFlow = remember { MutableStateFlow(captureButtonText) }
    val recentlyAddedProductsFlow = remember { MutableStateFlow(recentlyAddedProducts) }
    val missingProductsFlow = remember { MutableStateFlow(missingProducts) }
    val currentContextFlow = remember { MutableStateFlow(currentContext) }
    
    // Update flows when values change
    LaunchedEffect(detectedProducts) { detectedProductsFlow.value = detectedProducts }
    LaunchedEffect(currentProduct) { currentProductFlow.value = currentProduct }
    LaunchedEffect(detectionCount) { detectionCountFlow.value = detectionCount }
    LaunchedEffect(isCaptureMode) { isCaptureModeFlow.value = isCaptureMode }
    LaunchedEffect(captureButtonText) { captureButtonTextFlow.value = captureButtonText }
    LaunchedEffect(recentlyAddedProducts) { recentlyAddedProductsFlow.value = recentlyAddedProducts }
    LaunchedEffect(missingProducts) { missingProductsFlow.value = missingProducts }
    LaunchedEffect(currentContext) { currentContextFlow.value = currentContext }
    
    AppNavigation(
        hasPermission = hasPermission,
        selectedStore = selectedStore,
        detectedProducts = detectedProductsFlow,
        currentProduct = currentProductFlow,
        detectionCount = detectionCountFlow,
        isCaptureMode = isCaptureModeFlow,
        captureButtonText = captureButtonTextFlow,
        recentlyAddedProducts = recentlyAddedProductsFlow,
        missingProducts = missingProductsFlow,
        currentContext = currentContextFlow,
        onRequestPermission = onRequestPermission,
        onSaveDetections = onSaveDetections,
        onClearDetections = onClearDetections,
        onShareDetections = onShareDetections,
        onResetAntiSpamSession = onResetAntiSpamSession,
        onToggleCaptureMode = onToggleCaptureMode,
        onCaptureNow = onCaptureNow,
        products = products,
        productSearchQuery = productSearchQuery,
        selectedCategory = selectedCategory,
        onProductSearchQueryChange = { uiStateHolder.updateSearchQuery(it) },
        onCategoryChange = { uiStateHolder.updateSelectedCategory(it) },
        onAddToCart = { product ->
            // La lógica de agregar al carrito irá aquí
        },
        cartItems = cartItems,
        onUpdateCartQuantity = onUpdateCartQuantity,
        onRemoveFromCart = onRemoveFromCart,
        onCheckout = onCheckout,
        purchaseHistory = purchaseHistory,
        onStoreFilterChange = onStoreFilterChange,
        stores = stores,
        storeSearchQuery = storeSearchQuery,
        onStoreSelect = onStoreSelect,
        onStoreSearchQueryChange = { uiStateHolder.updateStoreSearchQuery(it) },
        cameraContent = cameraContent,
        headerSection = headerSection,
        detectionStats = detectionStats,
        currentProductCard = currentProductCard,
        missingProductsCard = missingProductsCard,
        productHistoryCard = productHistoryCard,
        floatingActionButtons = floatingActionButtons,
        permissionScreen = permissionScreen,
        productCard = productCard,
        cartItemRow = cartItemRow,
        purchaseHistoryCard = purchaseHistoryCard,
        storeCard = storeCard
    )
}