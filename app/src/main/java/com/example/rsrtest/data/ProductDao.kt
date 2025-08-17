package com.example.rsrtest.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): Product?

    @Query("SELECT * FROM products WHERE category = :category AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE brand = :brand AND isActive = 1 ORDER BY name ASC")
    fun getProductsByBrand(brand: String): Flow<List<Product>>

    @Query("""
        SELECT * FROM products 
        WHERE (name LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        OR keywords LIKE '%' || :query || '%'
        OR detectionKeywords LIKE '%' || :query || '%')
        AND isActive = 1
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("""
        SELECT * FROM products 
        WHERE detectionKeywords LIKE '%' || :keyword || '%'
        AND isActive = 1
        ORDER BY name ASC
        LIMIT 1
    """)
    suspend fun findProductByDetectionKeyword(keyword: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}

@Dao
interface CartDao {
    @Query("""
        SELECT p.id, p.name, p.brand, p.category, p.subcategory, p.description, 
               p.price, p.currency, p.barcode, p.imageUrl, p.weight, p.volume, 
               p.nutritionInfo, p.isActive, p.keywords, p.detectionKeywords,
               c.quantity, c.addedAt 
        FROM cart_items c 
        INNER JOIN products p ON c.productId = p.id 
        ORDER BY c.addedAt DESC
    """)
    fun getCartItemsWithProducts(): Flow<List<CartItemWithProduct>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId")
    suspend fun getCartItem(productId: String): CartItemEntity?

    @Query("SELECT SUM(quantity) FROM cart_items")
    fun getCartItemCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Update
    suspend fun updateCartItem(cartItem: CartItemEntity)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun removeFromCart(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_history ORDER BY purchaseDate DESC")
    fun getAllPurchases(): Flow<List<PurchaseHistory>>

    @Query("SELECT * FROM purchase_history WHERE id = :purchaseId")
    suspend fun getPurchaseById(purchaseId: String): PurchaseHistory?

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getPurchaseItems(purchaseId: String): List<PurchaseItem>

    @Query("""
        SELECT ph.*, pi.* FROM purchase_history ph
        LEFT JOIN purchase_items pi ON ph.id = pi.purchaseId
        WHERE ph.id = :purchaseId
    """)
    suspend fun getPurchaseWithItems(purchaseId: String): Map<PurchaseHistory, List<PurchaseItem>>

    @Query("""
        SELECT * FROM purchase_history 
        WHERE purchaseDate BETWEEN :startDate AND :endDate 
        ORDER BY purchaseDate DESC
    """)
    fun getPurchasesByDateRange(startDate: Long, endDate: Long): Flow<List<PurchaseHistory>>

    @Query("SELECT SUM(totalAmount) FROM purchase_history WHERE status = 'completed'")
    suspend fun getTotalSpent(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItem>)

    @Update
    suspend fun updatePurchase(purchase: PurchaseHistory)

    @Delete
    suspend fun deletePurchase(purchase: PurchaseHistory)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deletePurchaseItems(purchaseId: String)
}