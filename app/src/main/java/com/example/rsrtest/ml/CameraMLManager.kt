package com.example.rsrtest.ml

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.rsrtest.YoloDetectorTfLite
import com.example.rsrtest.data.DetectedProduct
import com.example.rsrtest.core.error.ErrorHandler
import com.example.rsrtest.core.offline.OfflineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraMLManager(
    private val context: Context,
    private val onDetection: (List<DetectedProduct>) -> Unit,
    private val errorHandler: ErrorHandler,
    private val offlineManager: OfflineManager? = null
) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val yoloDetector = YoloDetectorTfLite(context)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    companion object {
        private const val INFERENCE_INTERVAL_MS = 100L
        private const val DETECTION_LIST_THROTTLE_MS = 2000L
        private const val MAX_RETRIES = 3
    }
    
    private var lastAnalyzedTimestamp = 0L
    private var lastDetectionListUpdate = 0L
    private var consecutiveErrors = 0
    private var isRecovering = false
    
    fun getImageAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalyzer()
    }
    
    fun getCameraExecutor(): ExecutorService = cameraExecutor
    
    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        
        override fun analyze(imageProxy: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            
            if ((currentTimestamp - lastAnalyzedTimestamp) >= INFERENCE_INTERVAL_MS) {
                if (isRecovering) {
                    imageProxy.close()
                    return
                }
                
                try {
                    Log.d("CameraMLManager", "🔍 Iniciando análisis YOLO...")
                    
                    // Ejecutar detección YOLO con manejo de errores
                    val detections = performDetectionWithRetry(imageProxy)
                    Log.d("CameraMLManager", "🔍 YOLO detectó ${detections.size} objetos")
                    
                    if (detections.isNotEmpty()) {
                        processDetections(detections)
                    } else {
                        Log.d("CameraMLManager", "No hay detecciones para procesar")
                    }
                    
                    // Resetear contador de errores en operación exitosa
                    consecutiveErrors = 0
                    
                } catch (e: Exception) {
                    handleDetectionError(e)
                } finally {
                    imageProxy.close()
                    lastAnalyzedTimestamp = currentTimestamp
                }
            } else {
                imageProxy.close()
            }
        }
        
        private fun performDetectionWithRetry(imageProxy: ImageProxy): List<com.example.rsrtest.TfLiteDetection> {
            return try {
                yoloDetector.detectObjects(imageProxy)
            } catch (e: Exception) {
                consecutiveErrors++
                
                if (consecutiveErrors <= MAX_RETRIES) {
                    Log.w("CameraMLManager", "Reintentando detección (${consecutiveErrors}/$MAX_RETRIES)")
                    // Pequeña espera antes de reintentar
                    Thread.sleep(100 * consecutiveErrors.toLong())
                    yoloDetector.detectObjects(imageProxy)
                } else {
                    throw e
                }
            }
        }
        
        private fun processDetections(detections: List<com.example.rsrtest.TfLiteDetection>) {
            val pepsicoDetections = mutableListOf<DetectedProduct>()
            val uniqueDetections = mutableSetOf<String>()
            
            detections.forEach { detection ->
                val className = detection.className.lowercase()
                
                if (!uniqueDetections.contains(className)) {
                    Log.d("CameraMLManager", "🔄 Procesando detección NUEVA: $className con confianza ${detection.confidence}")
                    
                    uniqueDetections.add(className)
                    
                    val detectedProduct = DetectedProduct(
                        name = detection.className,
                        confidence = detection.confidence
                    )
                    pepsicoDetections.add(detectedProduct)
                    
                    // Registrar detección para sincronización offline si es necesario
                    offlineManager?.let { manager ->
                        if (!manager.isOfflineMode.value) {
                            // Enviar a sincronización si hay conexión
                            scope.launch {
                                manager.queueForSync(
                                    OfflineManager.SyncType.DETECTION_LOG,
                                    detectedProduct
                                )
                            }
                        }
                    }
                } else {
                    Log.d("CameraMLManager", "⏭️ Saltando detección duplicada en imagen: $className")
                }
            }
            
            // Throttle para evitar actualizaciones muy frecuentes
            if (pepsicoDetections.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastDetectionListUpdate >= DETECTION_LIST_THROTTLE_MS) {
                    Log.d("CameraMLManager", "✅ Enviando ${pepsicoDetections.size} detecciones")
                    onDetection(pepsicoDetections)
                    lastDetectionListUpdate = currentTime
                } else {
                    val remainingTime = DETECTION_LIST_THROTTLE_MS - (currentTime - lastDetectionListUpdate)
                    Log.d("CameraMLManager", "⏳ Detecciones en throttle, faltan ${remainingTime}ms")
                }
            }
        }
        
        private fun handleDetectionError(error: Exception) {
            Log.e("CameraMLManager", "Error en detección YOLO: ${error.message}", error)
            
            // Notificar error al manejador de errores
            errorHandler.handleError(error, "camera_ml_detection")
            
            // Si hay muchos errores consecutivos, intentar recuperación
            if (consecutiveErrors >= MAX_RETRIES) {
                attemptRecovery()
            }
        }
        
        private fun attemptRecovery() {
            isRecovering = true
            Log.w("CameraMLManager", "Iniciando recuperación del detector...")
            
            try {
                // Notificar error de ML
                errorHandler.handleCustomError(ErrorHandler.ErrorState.MLError)
                
                // Reiniciar detector
                yoloDetector.close()
                Thread.sleep(1000) // Esperar antes de reiniciar
                
                // El detector se reiniciará en la próxima detección
                consecutiveErrors = 0
                Log.d("CameraMLManager", "Recuperación completada")
                
            } catch (e: Exception) {
                Log.e("CameraMLManager", "Error en recuperación: ${e.message}", e)
            } finally {
                isRecovering = false
            }
        }
    }
    
    fun shutdown() {
        cameraExecutor.shutdown()
        yoloDetector.close()
    }
}