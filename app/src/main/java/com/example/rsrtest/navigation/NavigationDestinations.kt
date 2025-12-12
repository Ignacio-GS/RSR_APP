package com.example.rsrtest.navigation

sealed class NavigationDestination(val route: String) {
    object Scanner : NavigationDestination("scanner")
    object Products : NavigationDestination("products")
    object Cart : NavigationDestination("cart")
    object History : NavigationDestination("history")
    object Stores : NavigationDestination("stores")
}

data class BottomNavItem(
    val destination: NavigationDestination,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)