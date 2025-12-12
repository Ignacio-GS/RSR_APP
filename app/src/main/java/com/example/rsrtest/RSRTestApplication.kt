package com.example.rsrtest

import android.app.Application

/**
 * Application class principal para RSR Test App
 * 
 * Esta clase es el punto de entrada para la configuración de la aplicación.
 * Hilt está temporalmente deshabilitado por problemas de compatibilidad.
 * 
 * Responsabilidades:
 * - Configurar componentes globales de la aplicación
 * - Manejar ciclo de vida a nivel de aplicación
 * - Proveer acceso a dependencias globales (manualmente)
 */
class RSRTestApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Aquí se pueden inicializar componentes globales
        // como analytics, crash reporting, etc.
        
        // Temporalmente sin Hilt por problemas de compatibilidad
        // La dependency injection se manejará manualmente
    }
}