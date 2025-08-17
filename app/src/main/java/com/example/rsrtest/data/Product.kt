package com.example.rsrtest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val subcategory: String,
    val description: String,
    val price: Double,
    val currency: String = "MXN",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val weight: String? = null,
    val volume: String? = null,
    val nutritionInfo: String? = null,
    val isActive: Boolean = true,
    val keywords: String, // Palabras clave separadas por comas para búsqueda
    val detectionKeywords: String // Palabras clave para detección ML separadas por comas
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val productId: String,
    val quantity: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val purchaseDate: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val itemCount: Int,
    val status: String = "completed" // completed, pending, cancelled
)

@Entity(tableName = "purchase_items")
data class PurchaseItem(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val purchaseId: String,
    val productId: String,
    val productName: String,
    val productBrand: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

// Data class para mostrar compras con items
data class PurchaseWithItems(
    val id: String,
    val purchaseDate: Long,
    val totalAmount: Double,
    val itemCount: Int,
    val status: String,
    val items: List<PurchaseItem>
)

// Data class para la UI 
data class CartItemWithProduct(
    val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val subcategory: String,
    val description: String,
    val price: Double,
    val currency: String,
    val barcode: String?,
    val imageUrl: String?,
    val weight: String?,
    val volume: String?,
    val nutritionInfo: String?,
    val isActive: Boolean,
    val keywords: String,
    val detectionKeywords: String,
    val quantity: Int,
    val addedAt: Long
) {
    // Convertir a Product
    val product: Product
        get() = Product(
            id = id,
            name = name,
            brand = brand,
            category = category,
            subcategory = subcategory,
            description = description,
            price = price,
            currency = currency,
            barcode = barcode,
            imageUrl = imageUrl,
            weight = weight,
            volume = volume,
            nutritionInfo = nutritionInfo,
            isActive = isActive,
            keywords = keywords,
            detectionKeywords = detectionKeywords
        )
}

// Categorías de productos PepsiCo
enum class ProductCategory(val displayName: String) {
    BEBIDAS("Bebidas"),
    SNACKS("Snacks"),
    ALIMENTOS("Alimentos"),
    CEREALES("Cereales"),
    JUGOS("Jugos"),
    AGUA("Agua")
}

enum class ProductBrand(val displayName: String) {
    PEPSI("Pepsi"),
    DORITOS("Doritos"),
    CHEETOS("Cheetos"),
    LAYS("Lay's"),
    RUFFLES("Ruffles"),
    SABRITAS("Sabritas"),
    FRITOS("Fritos"),
    TOSTITOS("Tostitos"),
    GATORADE("Gatorade"),
    TROPICANA("Tropicana"),
    AQUAFINA("Aquafina"),
    SEVEN_UP("7UP"),
    MIRINDA("Mirinda"),
    MOUNTAIN_DEW("Mountain Dew"),
    LIPTON("Lipton"),
    H2OH("H2OH!"),
    MANZANITA_SOL("Manzanita Sol"),
    QUAKER("Quaker"),
    GAMESA("Gamesa"),
    EMPERADOR("Emperador")
}