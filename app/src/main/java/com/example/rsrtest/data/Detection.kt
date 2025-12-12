package com.example.rsrtest.data

import java.util.UUID

data class DetectedProduct(
    val name: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
)

data class MissingProduct(
    val name: String,
    val category: String,
    val confidence: Float, // qué tan seguro estamos de que falta
    val reason: String, // por qué creemos que falta
    val expectedLocation: String? = null
)

enum class ShelfContext(val displayName: String) {
    REFRIGERATOR("Refrigerador"),
    SNACK_SHELF("Estante de Snacks"),
    BEVERAGE_COOLER("Enfriador de Bebidas"),
    CHECKOUT_AREA("Área de Checkout"),
    CANDY_SECTION("Sección de Dulces"),
    UNKNOWN("Desconocido")
}

data class CartItem(
    val name: String,
    val quantity: Int = 1
)