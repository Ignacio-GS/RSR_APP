package com.example.rsrtest.core.ui

import com.example.rsrtest.data.*
import com.example.rsrtest.ml.CameraMLManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor centralizado de estado de la UI
 * 
 * Responsabilidades:
 * - Centralizar todo el estado de la aplicación
 * - Proporcionar StateFlows reactivos para la UI
 * - Gestionar operaciones de estado complejas
 * - Separar la lógica de estado de la UI
 */
class UIStateHolder(
    private val _productRepository: ProductRepository
) {
    
    // Getter para el productRepository
    val productRepository: ProductRepository get() = _productRepository
    
    // =============================================
    // Estados de Permisos
    // =============================================
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()
    
    fun setCameraPermission(hasPermission: Boolean) {
        _hasCameraPermission.value = hasPermission
    }
    
    // =============================================
    // Estados de Tiendas
    // =============================================
    private val _selectedStore = MutableStateFlow<Store?>(null)
    val selectedStore: StateFlow<Store?> = _selectedStore.asStateFlow()
    
    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()
    
    private val _storeSearchQuery = MutableStateFlow("")
    val storeSearchQuery: StateFlow<String> = _storeSearchQuery.asStateFlow()
    
    fun selectStore(store: Store) {
        _selectedStore.value = store
    }
    
    fun updateStores(storeList: List<Store>) {
        _stores.value = storeList
    }
    
    fun updateStoreSearchQuery(query: String) {
        _storeSearchQuery.value = query
    }
    
    // =============================================
    // Estados de Productos
    // =============================================
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()
    
    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    fun updateProducts(productList: List<Product>) {
        _products.value = productList
        updateFilteredProducts()
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        updateFilteredProducts()
    }
    
    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
        updateFilteredProducts()
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
    
    // =============================================
    // Estados de Carrito
    // =============================================
    private val _cartItems = MutableStateFlow<List<CartItemWithProduct>>(emptyList())
    val cartItems: StateFlow<List<CartItemWithProduct>> = _cartItems.asStateFlow()
    
    fun updateCartItems(items: List<CartItemWithProduct>) {
        _cartItems.value = items
    }
    
    // =============================================
    // Estados de Historial de Compras
    // =============================================
    private val _purchaseHistory = MutableStateFlow<List<PurchaseHistory>>(emptyList())
    val purchaseHistory: StateFlow<List<PurchaseHistory>> = _purchaseHistory.asStateFlow()
    
    fun updatePurchaseHistory(history: List<PurchaseHistory>) {
        _purchaseHistory.value = history
    }
    
    // =============================================
    // Estados de Captura y Detección
    // =============================================
    private val _captureButtonText = MutableStateFlow("📷")
    val captureButtonText: StateFlow<String> = _captureButtonText.asStateFlow()
    
    private val _recentlyAddedProducts = MutableStateFlow<Set<String>>(emptySet())
    val recentlyAddedProducts: StateFlow<Set<String>> = _recentlyAddedProducts.asStateFlow()
    
    private val _missingProducts = MutableStateFlow<List<MissingProduct>>(emptyList())
    val missingProducts: StateFlow<List<MissingProduct>> = _missingProducts.asStateFlow()
    
    private val _currentContext = MutableStateFlow(ShelfContext.UNKNOWN)
    val currentContext: StateFlow<ShelfContext> = _currentContext.asStateFlow()
    
    fun updateCaptureButtonText(text: String) {
        _captureButtonText.value = text
    }
    
    fun updateRecentlyAddedProducts(products: Set<String>) {
        _recentlyAddedProducts.value = products
    }
    
    fun updateMissingProducts(products: List<MissingProduct>) {
        _missingProducts.value = products
    }
    
    fun updateCurrentContext(context: ShelfContext) {
        _currentContext.value = context
    }
    
    // =============================================
    // Utilidades y Operaciones Comunes
    // =============================================
    
    /**
     * Obtiene el total de items en el carrito
     */
    fun getCartItemCount(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }
    
    /**
     * Obtiene el total del carrito
     */
    fun getCartTotal(): Double {
        return _cartItems.value.sumOf { it.price * it.quantity }
    }
    
    /**
     * Verifica si el carrito está vacío
     */
    fun isCartEmpty(): Boolean {
        return _cartItems.value.isEmpty()
    }
    
    /**
     * Limpia todos los estados (para logout o reset)
     */
    fun clearAllStates() {
        _selectedStore.value = null
        _searchQuery.value = ""
        _selectedCategory.value = null
        _storeSearchQuery.value = ""
        _captureButtonText.value = "📷"
        _recentlyAddedProducts.value = emptySet()
        _missingProducts.value = emptyList()
        _currentContext.value = ShelfContext.UNKNOWN
        updateFilteredProducts()
    }
    
    /**
     * Obtiene estadísticas simples para la UI
     */
    fun getUIStats(): UIStats {
        return UIStats(
            totalProducts = _products.value.size,
            filteredProducts = _filteredProducts.value.size,
            cartItems = getCartItemCount(),
            cartTotal = getCartTotal(),
            totalPurchases = _purchaseHistory.value.size,
            hasSelectedStore = _selectedStore.value != null
        )
    }
}

/**
 * Data class para estadísticas de la UI
 */
data class UIStats(
    val totalProducts: Int,
    val filteredProducts: Int,
    val cartItems: Int,
    val cartTotal: Double,
    val totalPurchases: Int,
    val hasSelectedStore: Boolean
)