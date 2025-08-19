package com.example.rsrtest.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val cartDao: CartDao,
    private val purchaseDao: PurchaseDao,
    private val storeDao: StoreDao
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
    
    suspend fun addToCart(productId: String, quantity: Int = 1) {
        val existingItem = cartDao.getCartItem(productId)
        if (existingItem != null) {
            cartDao.updateCartItem(
                existingItem.copy(quantity = existingItem.quantity + quantity)
            )
        } else {
            cartDao.insertCartItem(
                CartItemEntity(productId = productId, quantity = quantity)
            )
        }
    }
    
    suspend fun updateCartItemQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            cartDao.removeFromCart(productId)
        } else {
            val existingItem = cartDao.getCartItem(productId)
            if (existingItem != null) {
                cartDao.updateCartItem(existingItem.copy(quantity = quantity))
            }
        }
    }
    
    suspend fun removeFromCart(productId: String) = cartDao.removeFromCart(productId)
    
    suspend fun clearCart() = cartDao.clearCart()
    
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
    
    suspend fun completePurchase(cartItems: List<CartItemWithProduct>, storeId: String? = null): String {
        val purchaseId = java.util.UUID.randomUUID().toString()
        val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
        val itemCount = cartItems.sumOf { it.quantity }
        
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
        
        return purchaseId
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