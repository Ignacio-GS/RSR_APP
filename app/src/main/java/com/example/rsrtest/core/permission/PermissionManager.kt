package com.example.rsrtest.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interfaz para el manejo de permisos
 */
interface PermissionHandler {
    val hasCameraPermission: StateFlow<Boolean>
    val hasLocationPermission: StateFlow<Boolean>
    
    fun requestCameraPermission()
    fun requestLocationPermission()
    fun checkPermissions()
}

/**
 * Implementación del manejador de permisos
 * 
 * Responsabilidades:
 * - Gestionar el estado de los permisos
 * - Solicitar permisos cuando sea necesario
 * - Proporcionar flujos reactivos del estado de permisos
 * - Centralizar la lógica de permisos de la aplicación
 */
class PermissionManager(
    private val context: Context
) : PermissionHandler {
    
    // Estados de permisos como StateFlows reactivos
    private val _hasCameraPermission = MutableStateFlow(false)
    override val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()
    
    private val _hasLocationPermission = MutableStateFlow(false)
    override val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()
    
    // Launcher para permisos de cámara (será inicializado por la Activity)
    private var cameraPermissionLauncher: ((String) -> Unit)? = null
    
    // Launcher para permisos de ubicación (será inicializado por la Activity)
    private var locationPermissionLauncher: ((String) -> Unit)? = null
    
    /**
     * Inicializa los launchers de permisos desde la Activity
     */
    fun initializeLaunchers(
        cameraLauncher: (String) -> Unit,
        locationLauncher: (String) -> Unit
    ) {
        cameraPermissionLauncher = cameraLauncher
        locationPermissionLauncher = locationLauncher
    }
    
    /**
     * Verifica y actualiza el estado de todos los permisos
     */
    override fun checkPermissions() {
        updateCameraPermissionStatus()
        updateLocationPermissionStatus()
    }
    
    /**
     * Solicita permiso de cámara
     */
    override fun requestCameraPermission() {
        cameraPermissionLauncher?.invoke(Manifest.permission.CAMERA)
    }
    
    /**
     * Solicita permiso de ubicación
     */
    override fun requestLocationPermission() {
        locationPermissionLauncher?.invoke(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    /**
     * Maneja el resultado de la solicitud de permiso de cámara
     */
    fun handleCameraPermissionResult(isGranted: Boolean) {
        _hasCameraPermission.value = isGranted
        
        val message = if (isGranted) {
            "Permiso de cámara concedido"
        } else {
            "Permiso de cámara denegado"
        }
        
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        
        // Log para debugging
        android.util.Log.d("PermissionManager", "Camera permission result: $isGranted")
    }
    
    /**
     * Maneja el resultado de la solicitud de permiso de ubicación
     */
    fun handleLocationPermissionResult(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        
        val message = if (isGranted) {
            "Permiso de ubicación concedido"
        } else {
            "Permiso de ubicación denegado"
        }
        
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        
        // Log para debugging
        android.util.Log.d("PermissionManager", "Location permission result: $isGranted")
    }
    
    /**
     * Actualiza el estado del permiso de cámara
     */
    private fun updateCameraPermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        _hasCameraPermission.value = hasPermission
        android.util.Log.d("PermissionManager", "Camera permission status: $hasPermission")
    }
    
    /**
     * Actualiza el estado del permiso de ubicación
     */
    private fun updateLocationPermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _hasLocationPermission.value = hasPermission
        android.util.Log.d("PermissionManager", "Location permission status: $hasPermission")
    }
}

/**
 * Extensión para facilitar la configuración de permisos en Activities
 */
fun AppCompatActivity.setupPermissionManager(permissionManager: PermissionManager) {
    val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionManager.handleCameraPermissionResult(isGranted)
    }
    
    val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
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