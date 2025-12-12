package com.example.rsrtest.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.rsrtest.ui.screens.ScannerScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Rutas de navegación de la aplicación
 */
sealed class AppRoute(val route: String) {
    object Scanner : AppRoute("scanner")
    object Products : AppRoute("products")
    object Cart : AppRoute("cart")
    object History : AppRoute("history")
    object Stores : AppRoute("stores")
}

/**
 * Interfaz para el manejo de navegación
 */
interface NavigationHandler {
    val currentRoute: StateFlow<String>
    
    fun navigateTo(route: AppRoute)
    fun navigateBack()
    fun navigateToScanner()
    fun navigateToProducts()
    fun navigateToCart()
    fun navigateToHistory()
    fun navigateToStores()
    
    fun setNavController(navController: NavController)
}

/**
 * Implementación del manejador de navegación
 * 
 * Responsabilidades:
 * - Centralizar la lógica de navegación
 * - Proporcionar métodos seguros para navegar
 * - Mantener el estado de la navegación actual
 * - Manejar la configuración del NavController
 */
class AppNavigationHandler() : NavigationHandler {
    
    private val _currentRoute = MutableStateFlow(AppRoute.Scanner.route)
    override val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()
    
    private var navController: NavController? = null
    
    override fun setNavController(navController: NavController) {
        this.navController = navController
    }
    
    override fun navigateTo(route: AppRoute) {
        navController?.navigate(route.route) {
            // Opciones de navegación
            launchSingleTop = true
            restoreState = true
        }
        _currentRoute.value = route.route
    }
    
    override fun navigateBack() {
        navController?.popBackStack()
    }
    
    override fun navigateToScanner() {
        navigateTo(AppRoute.Scanner)
    }
    
    override fun navigateToProducts() {
        navigateTo(AppRoute.Products)
    }
    
    override fun navigateToCart() {
        navigateTo(AppRoute.Cart)
    }
    
    override fun navigateToHistory() {
        navigateTo(AppRoute.History)
    }
    
    override fun navigateToStores() {
        navigateTo(AppRoute.Stores)
    }
}

/**
 * Configuración del grafo de navegación
 * 
 * Esta función define todas las rutas y pantallas de la aplicación
 */
fun NavGraphBuilder.setupAppNavigation(
    navigationHandler: NavigationHandler,
    // Aquí se pasarían los parámetros necesarios para cada pantalla
    // Por ahora, estos serán implementados más tarde
) {
    
    composable(AppRoute.Scanner.route) {
        // ScannerScreen será implementada con los nuevos componentes
        // Por ahora, mantenemos la implementación existente
    }
    
    composable(AppRoute.Products.route) {
        // ProductsScreen será implementada
    }
    
    composable(AppRoute.Cart.route) {
        // CartScreen será implementada
    }
    
    composable(AppRoute.History.route) {
        // HistoryScreen será implementada
    }
    
    composable(AppRoute.Stores.route) {
        // StoresScreen será implementada
    }
}

/**
 * Extensión para configurar fácilmente la navegación en Activities
 */
fun androidx.activity.ComponentActivity.setupNavigation(
    navController: NavHostController,
    navigationHandler: NavigationHandler
) {
    navigationHandler.setNavController(navController)
    
    // Observar cambios en la navegación si es necesario
    navController.addOnDestinationChangedListener { _, destination, _ ->
        // Actualizar el estado actual si es necesario
        // Esto puede ser útil para analytics o logging
    }
}