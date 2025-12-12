package com.example.rsrtest.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rsrtest.data.Product
import com.example.rsrtest.data.Store
import com.example.rsrtest.data.CartItemWithProduct
import com.example.rsrtest.data.DetectedProduct
import com.example.rsrtest.data.PurchaseHistory
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    // Scanner Screen Dependencies
    hasPermission: Boolean,
    selectedStore: Store?,
    detectedProducts: StateFlow<List<DetectedProduct>>,
    currentProduct: StateFlow<DetectedProduct?>,
    detectionCount: StateFlow<Int>,
    isCaptureMode: StateFlow<Boolean>,
    captureButtonText: StateFlow<String>,
    recentlyAddedProducts: StateFlow<Set<String>>,
    missingProducts: StateFlow<List<com.example.rsrtest.data.MissingProduct>>,
    currentContext: StateFlow<com.example.rsrtest.data.ShelfContext>,
    onRequestPermission: () -> Unit,
    onSaveDetections: (List<DetectedProduct>) -> Unit,
    onClearDetections: () -> Unit,
    onShareDetections: (List<DetectedProduct>) -> Unit,
    onResetAntiSpamSession: () -> Unit,
    onToggleCaptureMode: () -> Unit,
    onCaptureNow: () -> Unit,
    
    // Products Screen Dependencies
    products: List<Product>,
    productSearchQuery: String,
    selectedCategory: String?,
    onProductSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onAddToCart: (Product) -> Unit,
    
    // Cart Screen Dependencies
    cartItems: List<CartItemWithProduct>,
    onUpdateCartQuantity: (String, Int) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onCheckout: () -> Unit,
    
    // History Screen Dependencies
    purchaseHistory: List<PurchaseHistory>,
    onStoreFilterChange: (String?) -> Unit,
    
    // Stores Screen Dependencies
    stores: List<Store>,
    storeSearchQuery: String,
    onStoreSelect: (Store?) -> Unit,
    onStoreSearchQueryChange: (String) -> Unit,
    
    // Composable Providers (for components that need to be passed from MainActivity)
    cameraContent: @Composable () -> Unit,
    headerSection: @Composable (Int, Store?) -> Unit,
    detectionStats: @Composable (List<DetectedProduct>) -> Unit,
    currentProductCard: @Composable (DetectedProduct) -> Unit,
    missingProductsCard: @Composable (List<com.example.rsrtest.data.MissingProduct>, com.example.rsrtest.data.ShelfContext) -> Unit,
    productHistoryCard: @Composable (List<DetectedProduct>, androidx.compose.foundation.lazy.LazyListState, Set<String>) -> Unit,
    floatingActionButtons: @Composable (onSave: () -> Unit, onClear: () -> Unit, onShare: () -> Unit) -> Unit,
    permissionScreen: @Composable (() -> Unit) -> Unit,
    productCard: @Composable (Product, (Product) -> Unit) -> Unit,
    cartItemRow: @Composable (CartItemWithProduct, (String, Int) -> Unit, (String) -> Unit) -> Unit,
    purchaseHistoryCard: @Composable (PurchaseHistory) -> Unit,
    storeCard: @Composable (Store, Store?, (Store?) -> Unit) -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem(
            destination = NavigationDestination.Products,
            icon = Icons.Default.Inventory,
            label = "Productos"
        ),
        BottomNavItem(
            destination = NavigationDestination.History,
            icon = Icons.Default.History,
            label = "Historial"
        ),
        BottomNavItem(
            destination = NavigationDestination.Scanner,
            icon = Icons.Default.CameraAlt,
            label = "Scanner"
        ),
        BottomNavItem(
            destination = NavigationDestination.Cart,
            icon = Icons.Default.ShoppingCart,
            label = "Carrito"
        ),
        BottomNavItem(
            destination = NavigationDestination.Stores,
            icon = Icons.Default.Store,
            label = "Tiendas"
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                items = bottomNavItems
            )
        }
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            paddingValues = paddingValues,
            hasPermission = hasPermission,
            selectedStore = selectedStore,
            detectedProducts = detectedProducts,
            currentProduct = currentProduct,
            detectionCount = detectionCount,
            isCaptureMode = isCaptureMode,
            captureButtonText = captureButtonText,
            recentlyAddedProducts = recentlyAddedProducts,
            missingProducts = missingProducts,
            currentContext = currentContext,
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
            onProductSearchQueryChange = onProductSearchQueryChange,
            onCategoryChange = onCategoryChange,
            onAddToCart = onAddToCart,
            cartItems = cartItems,
            onUpdateCartQuantity = onUpdateCartQuantity,
            onRemoveFromCart = onRemoveFromCart,
            onCheckout = onCheckout,
            purchaseHistory = purchaseHistory,
            onStoreFilterChange = onStoreFilterChange,
            stores = stores,
            storeSearchQuery = storeSearchQuery,
            onStoreSelect = onStoreSelect,
            onStoreSearchQueryChange = onStoreSearchQueryChange,
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
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.route == item.destination.route,
                onClick = {
                    navController.navigate(item.destination.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationRoute ?: NavigationDestination.Scanner.route) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    // All the same parameters as AppNavigation
    hasPermission: Boolean,
    selectedStore: Store?,
    detectedProducts: StateFlow<List<DetectedProduct>>,
    currentProduct: StateFlow<DetectedProduct?>,
    detectionCount: StateFlow<Int>,
    isCaptureMode: StateFlow<Boolean>,
    captureButtonText: StateFlow<String>,
    recentlyAddedProducts: StateFlow<Set<String>>,
    missingProducts: StateFlow<List<com.example.rsrtest.data.MissingProduct>>,
    currentContext: StateFlow<com.example.rsrtest.data.ShelfContext>,
    onRequestPermission: () -> Unit,
    onSaveDetections: (List<DetectedProduct>) -> Unit,
    onClearDetections: () -> Unit,
    onShareDetections: (List<DetectedProduct>) -> Unit,
    onResetAntiSpamSession: () -> Unit,
    onToggleCaptureMode: () -> Unit,
    onCaptureNow: () -> Unit,
    products: List<Product>,
    productSearchQuery: String,
    selectedCategory: String?,
    onProductSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onAddToCart: (Product) -> Unit,
    cartItems: List<CartItemWithProduct>,
    onUpdateCartQuantity: (String, Int) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onCheckout: () -> Unit,
    purchaseHistory: List<PurchaseHistory>,
    onStoreFilterChange: (String?) -> Unit,
    stores: List<Store>,
    storeSearchQuery: String,
    onStoreSelect: (Store?) -> Unit,
    onStoreSearchQueryChange: (String) -> Unit,
    cameraContent: @Composable () -> Unit,
    headerSection: @Composable (Int, Store?) -> Unit,
    detectionStats: @Composable (List<DetectedProduct>) -> Unit,
    currentProductCard: @Composable (DetectedProduct) -> Unit,
    missingProductsCard: @Composable (List<com.example.rsrtest.data.MissingProduct>, com.example.rsrtest.data.ShelfContext) -> Unit,
    productHistoryCard: @Composable (List<DetectedProduct>, androidx.compose.foundation.lazy.LazyListState, Set<String>) -> Unit,
    floatingActionButtons: @Composable (onSave: () -> Unit, onClear: () -> Unit, onShare: () -> Unit) -> Unit,
    permissionScreen: @Composable (() -> Unit) -> Unit,
    productCard: @Composable (Product, (Product) -> Unit) -> Unit,
    cartItemRow: @Composable (CartItemWithProduct, (String, Int) -> Unit, (String) -> Unit) -> Unit,
    purchaseHistoryCard: @Composable (PurchaseHistory) -> Unit,
    storeCard: @Composable (Store, Store?, (Store?) -> Unit) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Scanner.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(NavigationDestination.Scanner.route) {
            com.example.rsrtest.ui.screens.ScannerScreen(
                hasPermission = hasPermission,
                selectedStore = selectedStore,
                detectedProducts = detectedProducts,
                currentProduct = currentProduct,
                detectionCount = detectionCount,
                isCaptureMode = isCaptureMode,
                captureButtonText = captureButtonText,
                recentlyAddedProducts = recentlyAddedProducts,
                missingProducts = missingProducts,
                currentContext = currentContext,
                onRequestPermission = onRequestPermission,
                onSaveDetections = onSaveDetections,
                onClearDetections = onClearDetections,
                onShareDetections = onShareDetections,
                onResetAntiSpamSession = onResetAntiSpamSession,
                onToggleCaptureMode = onToggleCaptureMode,
                onCaptureNow = onCaptureNow,
                cameraContent = cameraContent,
                headerSection = headerSection,
                detectionStats = detectionStats,
                currentProductCard = currentProductCard,
                missingProductsCard = missingProductsCard,
                productHistoryCard = productHistoryCard,
                floatingActionButtons = floatingActionButtons,
                permissionScreen = permissionScreen
            )
        }
        
        composable(NavigationDestination.Products.route) {
            com.example.rsrtest.ui.screens.ProductsScreen(
                products = products,
                searchQuery = productSearchQuery,
                selectedCategory = selectedCategory,
                onSearchQueryChange = onProductSearchQueryChange,
                onCategoryChange = onCategoryChange,
                onAddToCart = onAddToCart
            )
        }
        
        composable(NavigationDestination.Cart.route) {
            com.example.rsrtest.ui.screens.CartScreen(
                cartItems = cartItems,
                selectedStore = selectedStore,
                onUpdateQuantity = onUpdateCartQuantity,
                onRemoveItem = onRemoveFromCart,
                onCheckout = onCheckout
            )
        }
        
        composable(NavigationDestination.History.route) {
            com.example.rsrtest.ui.screens.HistoryScreen(
                purchaseHistory = purchaseHistory,
                onStoreFilterChange = onStoreFilterChange
            )
        }
        
        composable(NavigationDestination.Stores.route) {
            com.example.rsrtest.ui.screens.StoresScreen(
                stores = stores,
                selectedStore = selectedStore,
                searchQuery = storeSearchQuery,
                onStoreSelect = onStoreSelect,
                onSearchQueryChange = onStoreSearchQueryChange
            )
        }
    }
}