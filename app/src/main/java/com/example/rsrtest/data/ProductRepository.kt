package com.example.rsrtest.data

import kotlinx.coroutines.flow.Flow
import android.util.Log
import com.example.rsrtest.core.error.ErrorHandler
import com.example.rsrtest.core.offline.OfflineManager

class ProductRepository(
    private val productDao: ProductDao,
    private val cartDao: CartDao,
    private val purchaseDao: PurchaseDao,
    private val storeDao: StoreDao,
    private val errorHandler: ErrorHandler? = null,
    private val offlineManager: OfflineManager? = null
) {
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    
    fun getProductsByCategory(category: String): Flow<List<Product>> = 
        productDao.getProductsByCategory(category)
    
    fun getProductsByBrand(brand: String): Flow<List<Product>> = 
        productDao.getProductsByBrand(brand)
    
    fun searchProducts(query: String): Flow<List<Product>> = 
        productDao.searchProducts(query)
    
    suspend fun getProductById(productId: String): Product? = 
        productDao.getProductById(productId)
    
    suspend fun findProductByDetectionKeyword(keyword: String): Product? = 
        productDao.findProductByDetectionKeyword(keyword)
    
    suspend fun insertProducts(products: List<Product>) = 
        productDao.insertProducts(products)
    
    // Cart operations
    fun getCartItems(): Flow<List<CartItemWithProduct>> = 
        cartDao.getCartItemsWithProducts()
    
    fun getCartItemCount(): Flow<Int> = cartDao.getCartItemCount()
    
    suspend fun addToCart(productId: String, quantity: Int = 1): Result<Boolean> {
        return try {
            // Validar parámetros
            if (productId.isBlank()) {
                return Result.failure(IllegalArgumentException("Product ID cannot be blank"))
            }
            if (quantity <= 0) {
                return Result.failure(IllegalArgumentException("Quantity must be positive"))
            }
            
            // Verificar que el producto existe
            val product = getProductById(productId)
            if (product == null) {
                return Result.failure(IllegalArgumentException("Product not found: $productId"))
            }
            
            val existingItem = cartDao.getCartItem(productId)
            if (existingItem != null) {
                val newQuantity = existingItem.quantity + quantity
                if (newQuantity > 100) { // Límite de seguridad
                    return Result.failure(IllegalArgumentException("Quantity limit exceeded"))
                }
                cartDao.updateCartItem(
                    existingItem.copy(quantity = newQuantity)
                )
            } else {
                cartDao.insertCartItem(
                    CartItemEntity(productId = productId, quantity = quantity)
                )
            }
            
            Log.d("ProductRepository", "Product added to cart: $productId, quantity: $quantity")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error adding to cart: $productId", e)
            errorHandler?.handleError(e, "add_to_cart")
            Result.failure(e)
        }
    }
    
    suspend fun updateCartItemQuantity(productId: String, quantity: Int): Result<Boolean> {
        return try {
            if (productId.isBlank()) {
                return Result.failure(IllegalArgumentException("Product ID cannot be blank"))
            }
            
            if (quantity <= 0) {
                cartDao.removeFromCart(productId)
                Log.d("ProductRepository", "Product removed from cart: $productId")
            } else {
                if (quantity > 100) {
                    return Result.failure(IllegalArgumentException("Quantity limit exceeded"))
                }
                
                val existingItem = cartDao.getCartItem(productId)
                if (existingItem != null) {
                    cartDao.updateCartItem(existingItem.copy(quantity = quantity))
                    Log.d("ProductRepository", "Cart item updated: $productId, quantity: $quantity")
                } else {
                    return Result.failure(IllegalArgumentException("Cart item not found: $productId"))
                }
            }
            
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error updating cart item: $productId", e)
            errorHandler?.handleError(e, "update_cart_quantity")
            Result.failure(e)
        }
    }
    
    suspend fun removeFromCart(productId: String): Result<Boolean> {
        return try {
            if (productId.isBlank()) {
                return Result.failure(IllegalArgumentException("Product ID cannot be blank"))
            }
            
            cartDao.removeFromCart(productId)
            Log.d("ProductRepository", "Product removed from cart: $productId")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error removing from cart: $productId", e)
            errorHandler?.handleError(e, "remove_from_cart")
            Result.failure(e)
        }
    }
    
    suspend fun clearCart(): Result<Boolean> {
        return try {
            cartDao.clearCart()
            Log.d("ProductRepository", "Cart cleared")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error clearing cart", e)
            errorHandler?.handleError(e, "clear_cart")
            Result.failure(e)
        }
    }
    
    // Purchase operations
    fun getAllPurchases(): Flow<List<PurchaseHistory>> = purchaseDao.getAllPurchases()
    
    suspend fun getPurchaseById(purchaseId: String): PurchaseHistory? = 
        purchaseDao.getPurchaseById(purchaseId)
    
    suspend fun getPurchaseItems(purchaseId: String): List<PurchaseItem> = 
        purchaseDao.getPurchaseItems(purchaseId)
    
    fun getPurchasesByDateRange(startDate: Long, endDate: Long): Flow<List<PurchaseHistory>> = 
        purchaseDao.getPurchasesByDateRange(startDate, endDate)
    
    suspend fun getTotalSpent(): Double = purchaseDao.getTotalSpent() ?: 0.0
    
    fun getPurchasesByStore(storeId: String): Flow<List<PurchaseHistory>> = 
        purchaseDao.getPurchasesByStore(storeId)
    
    suspend fun completePurchase(cartItems: List<CartItemWithProduct>, storeId: String? = null): Result<String> {
        return try {
            // Validaciones
            if (cartItems.isEmpty()) {
                return Result.failure(IllegalArgumentException("Cart cannot be empty"))
            }
            
            if (cartItems.size > 50) {
                return Result.failure(IllegalArgumentException("Too many items in cart"))
            }
            
            // Validar cada item
            for (item in cartItems) {
                if (item.quantity <= 0 || item.quantity > 100) {
                    return Result.failure(IllegalArgumentException("Invalid quantity for ${item.name}"))
                }
                if (item.price <= 0 || item.price > 10000) {
                    return Result.failure(IllegalArgumentException("Invalid price for ${item.name}"))
                }
            }
            
            val purchaseId = java.util.UUID.randomUUID().toString()
            val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
            val itemCount = cartItems.sumOf { it.quantity }
            
            // Validar total
            if (totalAmount <= 0 || totalAmount > 100000) {
                return Result.failure(IllegalArgumentException("Invalid total amount"))
            }
            
            // Crear historial de compra
            val purchase = PurchaseHistory(
                id = purchaseId,
                totalAmount = totalAmount,
                itemCount = itemCount,
                storeId = storeId
            )
            
            // Crear items de compra
            val purchaseItems = cartItems.map { cartItem ->
                PurchaseItem(
                    purchaseId = purchaseId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    productBrand = cartItem.product.brand,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.product.price,
                    totalPrice = cartItem.product.price * cartItem.quantity
                )
            }
            
            // Guardar en base de datos
            purchaseDao.insertPurchase(purchase)
            purchaseDao.insertPurchaseItems(purchaseItems)
            
            // Limpiar carrito
            clearCart()
            
            // Si hay conexión, sincronizar con la nube
            offlineManager?.let { manager ->
                if (manager.isOfflineMode.value) {
                    // Encolar para sincronización cuando haya conexión
                    manager.queueForSync(
                        OfflineManager.SyncType.PURCHASE_CREATE,
                        purchase
                    )
                } else {
                    // Intentar sincronizar inmediatamente
                    try {
                        manager.queueForSync(
                            OfflineManager.SyncType.PURCHASE_CREATE,
                            purchase
                        )
                    } catch (e: Exception) {
                        Log.w("ProductRepository", "Failed to sync purchase, queued for later: ${e.message}")
                    }
                }
            }
            
            Log.d("ProductRepository", "Purchase completed: $purchaseId, total: $$totalAmount")
            Result.success(purchaseId)
            
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error completing purchase", e)
            errorHandler?.handleError(e, "complete_purchase")
            Result.failure(e)
        }
    }
    
    suspend fun deletePurchase(purchaseId: String) {
        val purchase = purchaseDao.getPurchaseById(purchaseId)
        purchase?.let {
            purchaseDao.deletePurchase(it)
            purchaseDao.deletePurchaseItems(purchaseId)
        }
    }

    // Store operations
    fun getAllStores(): Flow<List<Store>> = storeDao.getAllStores()
    
    suspend fun getStoreById(customerNumber: String): Store? = 
        storeDao.getStoreById(customerNumber)
    
    fun getStoresByChain(chain: String): Flow<List<Store>> = 
        storeDao.getStoresByChain(chain)
    
    fun searchStores(query: String): Flow<List<Store>> = 
        storeDao.searchStores(query)
    
    suspend fun insertStores(stores: List<Store>) = 
        storeDao.insertStores(stores)
}