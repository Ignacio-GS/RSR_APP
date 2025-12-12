package com.example.rsrtest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.RectF
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

data class TfLiteDetection(
    val bbox: RectF,
    val confidence: Float,
    val classId: Int,
    val className: String
)

class YoloDetectorTfLite(private val context: Context) {
    
    companion object {
        private const val TAG = "YoloDetectorTfLite"
        private const val MODEL_FILENAME = "best_float32.tflite"  // Tu modelo YOLO convertido
        private const val LABELS_FILENAME = "classes.txt"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.25f  // Umbral más bajo para más detecciones
        private const val IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTION = 8400  // Según la salida [1, 11, 8400]
        private const val OUTPUT_CLASSES = 7    // 11 - 4 = 7 clases
    }
    
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isInitialized = false
    
    // Thresholds simplificados por clase
    private val classSpecificThresholds = mapOf(
        "7up" to 0.20f,
        "Mirinda" to 0.20f,
        "Squirt" to 0.20f,
        "Pepsi Black" to 0.25f,
        "Pepsi" to 0.20f,
        "Cheetos" to 0.25f,
        "Manzanita Sol" to 0.20f
    )
    
    // Boost de confianza simplificado
    private val confidenceBoostFactors = mapOf(
        "7up" to 1.10f,
        "Mirinda" to 1.10f,
        "Squirt" to 1.10f,
        "Pepsi Black" to 0.95f
    )
    
    // Método para obtener threshold específico de una clase
    private fun getThresholdForClass(className: String): Float {
        return classSpecificThresholds[className] ?: CONFIDENCE_THRESHOLD
    }
    
    // Método para aplicar boost de confianza
    private fun applyConfidenceBoost(confidence: Float, className: String): Float {
        val boostFactor = confidenceBoostFactors[className] ?: 1.0f
        return minOf(1.0f, confidence * boostFactor) // No superar 1.0
    }
    
    init {
        initializeModel()
    }
    
    private fun initializeModel() {
        try {
            Log.d(TAG, "Iniciando carga del modelo TensorFlow Lite...")
            
            // Verificar que el archivo existe
            try {
                val assetList = context.assets.list("")
                Log.d(TAG, "Archivos en assets: ${assetList?.joinToString(", ")}")
                
                val assetFile = context.assets.open(MODEL_FILENAME)
                Log.d(TAG, "Archivo $MODEL_FILENAME encontrado, tamaño: ${assetFile.available()} bytes")
                assetFile.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error verificando archivo del modelo: ${e.message}", e)
                throw e
            }
            
            // Cargar modelo TensorFlow Lite
            Log.d(TAG, "Cargando modelo desde assets...")
            val modelBuffer = loadModelFile()
            Log.d(TAG, "Modelo cargado en buffer, tamaño: ${modelBuffer.capacity()} bytes")
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                Log.d(TAG, "Configurando intérprete con 4 threads")
                // Optionally use GPU delegate
                // addDelegate(GpuDelegate())
            }
            
            Log.d(TAG, "Creando intérprete TensorFlow Lite...")
            interpreter = Interpreter(modelBuffer, options)
            
            // Verificar dimensiones del modelo
            val inputShape = interpreter!!.getInputTensor(0).shape()
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            Log.d(TAG, "Dimensiones entrada: ${inputShape.contentToString()}")
            Log.d(TAG, "Dimensiones salida: ${outputShape.contentToString()}")
            
            // Cargar etiquetas
            Log.d(TAG, "Cargando etiquetas...")
            labels = loadLabels()
            
            isInitialized = true
            Log.d(TAG, "✅ YOLO TensorFlow Lite modelo inicializado exitosamente")
            Log.d(TAG, "Clases disponibles: ${labels.size}")
            Log.d(TAG, "Clases: $labels")
            
            // Probar inferencia inmediatamente
            val testResult = testModelInference()
            Log.d(TAG, "Resultado prueba de inferencia: $testResult")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando YOLO TfLite: ${e.message}", e)
            e.printStackTrace()
            isInitialized = false
        }
    }
    
    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    private fun loadLabels(): List<String> {
        return try {
            context.assets.open(LABELS_FILENAME).bufferedReader().readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo cargar $LABELS_FILENAME, usando clases por defecto")
            listOf("7up", "Cheetos", "Manzanita Sol", "Mirinda", "Pepsi", "Pepsi Black", "Squirt")
        }
    }
    
    // Método de prueba para simular detecciones
    fun simulateDetection(): List<TfLiteDetection> {
        Log.d(TAG, "Simulando detección para pruebas...")
        return listOf(
            TfLiteDetection(
                bbox = android.graphics.RectF(100f, 100f, 200f, 200f),
                confidence = 0.85f,
                classId = 0,
                className = "Pepsi"
            )
        )
    }

    // Método para probar el modelo con datos dummy
    fun testModelInference(): Boolean {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "No se puede probar: modelo no inicializado")
            return false
        }
        
        return try {
            Log.d(TAG, "Probando inferencia con datos dummy...")
            
            // Crear datos de entrada dummy (imagen negra de 640x640)
            val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
            inputBuffer.order(ByteOrder.nativeOrder())
            // Llenar con ceros (imagen negra)
            for (i in 0 until INPUT_SIZE * INPUT_SIZE * 3) {
                inputBuffer.putFloat(0f)
            }
            
            // Buffer de salida - formato [1, 11, 8400]
            val outputBuffer = Array(1) { Array(11) { FloatArray(MAX_DETECTION) } }
            
            // Ejecutar inferencia
            interpreter!!.run(inputBuffer, outputBuffer)
            
            Log.d(TAG, "✅ Inferencia de prueba exitosa")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en inferencia de prueba: ${e.message}", e)
            false
        }
    }

    fun detectObjects(imageProxy: ImageProxy): List<TfLiteDetection> {
        Log.d(TAG, "🔍 detectObjects called - isInitialized: $isInitialized, interpreter: ${interpreter != null}")
        
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "❌ Detector no inicializado - isInitialized: $isInitialized, interpreter: ${interpreter != null}")
            throw IllegalStateException("YOLO detector not initialized")
        }
        
        return try {
            Log.d(TAG, "Iniciando proceso de detección...")
            
            // Validar imagen
            if (imageProxy.width <= 0 || imageProxy.height <= 0) {
                throw IllegalArgumentException("Invalid image dimensions: ${imageProxy.width}x${imageProxy.height}")
            }
            
            // Convertir ImageProxy a Bitmap con manejo de memoria
            val bitmap = imageProxyToBitmap(imageProxy)
            Log.d(TAG, "Bitmap creado: ${bitmap.width}x${bitmap.height}")
            
            // Validar bitmap
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                throw IllegalStateException("Invalid bitmap dimensions")
            }
            
            // Redimensionar bitmap con manejo de memoria
            val resizedBitmap = try {
                Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            } catch (e: Exception) {
                throw RuntimeException("Failed to resize bitmap", e)
            }
            Log.d(TAG, "Bitmap redimensionado: ${resizedBitmap.width}x${resizedBitmap.height}")
            
            try {
                // Convertir bitmap a ByteBuffer para TensorFlow Lite
                val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
                Log.d(TAG, "ByteBuffer creado, tamaño: ${inputBuffer.capacity()}")
                
                // Validar buffer
                if (inputBuffer.capacity() != 4 * INPUT_SIZE * INPUT_SIZE * 3) {
                    throw IllegalStateException("Invalid input buffer size: ${inputBuffer.capacity()}")
                }
                
                // Preparar buffer de salida - formato [1, 11, 8400]
                val outputBuffer = Array(1) { Array(11) { FloatArray(MAX_DETECTION) } }
                Log.d(TAG, "Buffer de salida preparado: [1, 11, $MAX_DETECTION]")
                
                // Ejecutar inferencia con timeout
                Log.d(TAG, "Ejecutando inferencia TensorFlow Lite...")
                val inferenceStart = System.currentTimeMillis()
                interpreter!!.run(inputBuffer, outputBuffer)
                val inferenceTime = System.currentTimeMillis() - inferenceStart
                Log.d(TAG, "Inferencia completada en ${inferenceTime}ms")
                
                // Validar tiempos de inferencia
                if (inferenceTime > 1000) { // Más de 1 segundo es demasiado lento
                    Log.w(TAG, "Inference too slow: ${inferenceTime}ms")
                }
                
                // Procesar resultados
                val detections = processOutputs(outputBuffer[0], bitmap.width, bitmap.height)
                
                Log.d(TAG, "Procesamiento completado - Detectados ${detections.size} objetos")
                for (detection in detections.take(3)) { // Limitar logging
                    Log.d(TAG, "Detección: ${detection.className} (${detection.confidence}) en ${detection.bbox}")
                }
                
                detections
                
            } finally {
                // Liberar memoria de bitmaps
                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                bitmap.recycle()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en detección: ${e.message}", e)
            throw RuntimeException("YOLO detection failed", e)
        }
    }
    
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        return when (imageProxy.format) {
            ImageFormat.YUV_420_888 -> {
                val yuvImage = YuvImage(
                    imageProxyToByteArray(imageProxy),
                    ImageFormat.NV21,
                    imageProxy.width,
                    imageProxy.height,
                    null
                )
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(
                    android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                    100,
                    out
                )
                val imageBytes = out.toByteArray()
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
            else -> {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }
    
    private fun imageProxyToByteArray(imageProxy: ImageProxy): ByteArray {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        return nv21
    }
    
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        Log.d(TAG, "Convirtiendo bitmap ${bitmap.width}x${bitmap.height} a ByteBuffer")
        
        var pixel = 0
        var samplePixels = mutableListOf<Triple<Float, Float, Float>>()
        
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = intValues[pixel++]
                
                // Extraer componentes RGB
                val red = ((pixelValue shr 16) and 0xFF) / 255.0f
                val green = ((pixelValue shr 8) and 0xFF) / 255.0f  
                val blue = (pixelValue and 0xFF) / 255.0f
                
                // PRUEBA 1: Normalización simple [0, 1] sin ImageNet
                val normalizedRed = red
                val normalizedGreen = green
                val normalizedBlue = blue
                
                // PRUEBA 2: Si la anterior no funciona, probar ImageNet
                // val normalizedRed = (red - 0.485f) / 0.229f
                // val normalizedGreen = (green - 0.456f) / 0.224f
                // val normalizedBlue = (blue - 0.406f) / 0.225f
                
                byteBuffer.putFloat(normalizedRed)
                byteBuffer.putFloat(normalizedGreen)
                byteBuffer.putFloat(normalizedBlue)
                
                // Guardar muestra de los primeros pixels para debugging
                if (pixel <= 5) {
                    samplePixels.add(Triple(normalizedRed, normalizedGreen, normalizedBlue))
                }
            }
        }
        
        Log.d(TAG, "Muestra de pixels normalizados: $samplePixels")
        
        return byteBuffer
    }
    
    private fun processOutputs(
        outputs: Array<FloatArray>,
        originalWidth: Int,
        originalHeight: Int
    ): List<TfLiteDetection> {
        val detections = mutableListOf<TfLiteDetection>()
        
        Log.d(TAG, "Procesando salidas con formato [11, $MAX_DETECTION]")
        
        // El formato de salida es [1, 11, 8400] donde:
        // - 11 = [x, y, w, h, confidence, class1, class2, ..., class7] 
        // - 8400 = número de detecciones posibles
        
        // DEBUG: Solo mostrar si hay detecciones importantes
        var hasHighConfidence = false
        for (i in 0 until minOf(10, MAX_DETECTION)) {
            if (outputs[4][i] > 0.2f) {
                hasHighConfidence = true
                break
            }
        }
        
        if (hasHighConfidence) {
            Log.d(TAG, "Detecciones con alta confianza encontradas")
        }
        
        // DEBUG: Solo mostrar max confianza si es relevante
        var maxConfidenceFound = 0f
        for (i in 0 until minOf(100, MAX_DETECTION)) {
            val confidence = outputs[4][i]
            if (confidence > maxConfidenceFound) {
                maxConfidenceFound = confidence
            }
        }
        
        if (maxConfidenceFound > 0.1f) {
            Log.d(TAG, "Máxima confianza: $maxConfidenceFound")
        }
        
        for (i in 0 until MAX_DETECTION) {
            try {
                val centerX = outputs[0][i]      // x
                val centerY = outputs[1][i]      // y  
                val width = outputs[2][i]        // w
                val height = outputs[3][i]       // h
                
                // PRUEBA: Probar ambos formatos
                // Formato 1: [x, y, w, h, objectness, class0...class6]
                var finalConfidence1 = 0f
                var classId1 = 0
                if (outputs.size > 4) {
                    val objectConfidence = outputs[4][i]
                    var maxClassProb = 0f
                    for (j in 5 until 11) {
                        val classProb = outputs[j][i]
                        if (classProb > maxClassProb) {
                            maxClassProb = classProb
                            classId1 = j - 5
                        }
                    }
                    finalConfidence1 = objectConfidence * maxClassProb
                }
                
                // Formato 2: [x, y, w, h, class0...class6] (sin objectness separado)
                var finalConfidence2 = 0f
                var classId2 = 0
                for (j in 4 until 11) {
                    val classProb = outputs[j][i] 
                    if (classProb > finalConfidence2) {
                        finalConfidence2 = classProb
                        classId2 = j - 4
                    }
                }
                
                // Usar el formato que dé mayor confianza
                var finalConfidence = if (finalConfidence1 > finalConfidence2) finalConfidence1 else finalConfidence2
                val classId = if (finalConfidence1 > finalConfidence2) classId1 else classId2
                
                // Aplicar boost de confianza para clases específicas
                if (classId < labels.size) {
                    val className = labels.getOrElse(classId) { "unknown_$classId" }
                    val originalConfidence = finalConfidence
                    finalConfidence = applyConfidenceBoost(finalConfidence, className)
                    
                    // Log boost aplicado para productos objetivo
                    if (className in listOf("7up", "Mirinda", "Squirt") && originalConfidence != finalConfidence) {
                        Log.d(TAG, "Boost aplicado a $className: $originalConfidence -> $finalConfidence")
                    }
                }
                
                // Solo debug para detecciones con alta confianza
                if (finalConfidence > 0.2f) {
                    Log.d(TAG, "Detección strong: conf=$finalConfidence cls=$classId (${labels.getOrElse(classId) { "unknown" }})")
                }
                
                if (classId < labels.size) {
                    val className = labels.getOrElse(classId) { "unknown_$classId" }
                    val specificThreshold = getThresholdForClass(className) // Usar threshold simple
                    
                    if (finalConfidence > specificThreshold) {
                        // Convertir coordenadas normalizadas a píxeles
                        val scaleX = originalWidth.toFloat() / INPUT_SIZE
                        val scaleY = originalHeight.toFloat() / INPUT_SIZE
                        
                        val scaledCenterX = centerX * scaleX
                        val scaledCenterY = centerY * scaleY
                        val scaledWidth = width * scaleX
                        val scaledHeight = height * scaleY
                        
                        val left = scaledCenterX - scaledWidth / 2
                        val top = scaledCenterY - scaledHeight / 2
                        val right = scaledCenterX + scaledWidth / 2
                        val bottom = scaledCenterY + scaledHeight / 2
                        
                        val bbox = RectF(
                            max(0f, left),
                            max(0f, top),
                            min(originalWidth.toFloat(), right),
                            min(originalHeight.toFloat(), bottom)
                        )
                        
                        detections.add(
                            TfLiteDetection(
                                bbox = bbox,
                                confidence = finalConfidence,
                                classId = classId,
                                className = className
                            )
                        )
                        
                        // Actualizar estadísticas
                        updateDetectionStats(className)
                        
                        Log.d(TAG, "Detección encontrada: $className con confianza $finalConfidence (threshold: $specificThreshold)")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error procesando detección $i: ${e.message}")
            }
        }
        
        Log.d(TAG, "Total detecciones antes de NMS: ${detections.size}")
        val nmsDetections = applyNMS(detections)
        
        // Filtro simplificado - permitir más detecciones
        val finalDetections = mutableListOf<TfLiteDetection>()
        for (detection in nmsDetections) {
            // Solo aplicar threshold básico
            if (detection.confidence > 0.15f) {
                finalDetections.add(detection)
                Log.d(TAG, "✅ Detección aceptada: ${detection.className} (${detection.confidence})")
            } else {
                Log.d(TAG, "❌ Detección filtrada: ${detection.className} (${detection.confidence})")
            }
        }
        
        Log.d(TAG, "Detecciones finales: ${finalDetections.size}/${nmsDetections.size}")
        return finalDetections
    }
    
    private fun applyNMS(detections: List<TfLiteDetection>): List<TfLiteDetection> {
        if (detections.isEmpty()) return emptyList()
        
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<TfLiteDetection>()
        
        for (detection in sortedDetections) {
            var shouldSelect = true
            
            for (selectedDetection in selectedDetections) {
                val iou = calculateIoU(detection.bbox, selectedDetection.bbox)
                if (iou > IOU_THRESHOLD) {
                    shouldSelect = false
                    break
                }
            }
            
            if (shouldSelect) {
                selectedDetections.add(detection)
            }
        }
        
        return selectedDetections
    }
    
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)
        
        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }
    
    // Estadísticas de detección para monitoreo
    private val detectionStats = mutableMapOf<String, Int>()
    private var totalDetections = 0
    
    // Sistema de estabilización simplificado
    private val detectionHistory = mutableMapOf<String, MutableList<Long>>()
    private val STABILITY_WINDOW_MS = 500L // 0.5 segundos
    private val MIN_CONSISTENT_DETECTIONS = 1 // Solo 1 detección
    
    // Verificar si una detección es estable
    private fun isStableDetection(className: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val history = detectionHistory.getOrPut(className) { mutableListOf() }
        
        // Agregar detección actual
        history.add(currentTime)
        
        // Limpiar detecciones viejas
        history.removeAll { currentTime - it > STABILITY_WINDOW_MS }
        
        // Verificar estabilidad
        val isStable = history.size >= MIN_CONSISTENT_DETECTIONS
        
        if (isStable) {
            Log.d(TAG, "✅ Detección estable para $className (${history.size} en ${STABILITY_WINDOW_MS}ms)")
        } else {
            Log.d(TAG, "⏳ Detección inestable para $className (${history.size}/${MIN_CONSISTENT_DETECTIONS})")
        }
        
        return isStable
    }
    
    // Sistema de balance dinámico
    private fun getDynamicThreshold(className: String): Float {
        val baseThreshold = getThresholdForClass(className)
        
        // Si Pepsi Black domina más del 60% de detecciones, aumentar su threshold
        if (className == "Pepsi Black" && totalDetections > 10) {
            val pepsiBlackCount = detectionStats["Pepsi Black"] ?: 0
            val pepsiBlackRatio = pepsiBlackCount.toFloat() / totalDetections
            
            if (pepsiBlackRatio > 0.6f) {
                val penalty = (pepsiBlackRatio - 0.4f) * 0.5f // Hasta 0.1 de penalización
                val penalizedThreshold = baseThreshold + penalty
                Log.d(TAG, "Penalizando Pepsi Black: ratio=$pepsiBlackRatio, threshold=$baseThreshold -> $penalizedThreshold")
                return penalizedThreshold
            }
        }
        
        return baseThreshold
    }
    
    fun getDetectionStats(): Map<String, Int> = detectionStats.toMap()
    
    fun resetDetectionStats() {
        detectionStats.clear()
        totalDetections = 0
        detectionHistory.clear() // También limpiar historial de estabilidad
        Log.d(TAG, "Estadísticas y historial de estabilidad reiniciados")
    }
    
    private fun updateDetectionStats(className: String) {
        detectionStats[className] = detectionStats.getOrDefault(className, 0) + 1
        totalDetections++
    }
    
    fun logDetectionStats() {
        Log.d(TAG, "=== ESTADÍSTICAS DE DETECCIÓN ===")
        Log.d(TAG, "Total detecciones: $totalDetections")
        detectionStats.forEach { (className, count) ->
            val percentage = if (totalDetections > 0) (count * 100.0f / totalDetections) else 0f
            val historySize = detectionHistory[className]?.size ?: 0
            Log.d(TAG, "$className: $count detecciones (${String.format("%.1f", percentage)}%) - Historia: $historySize")
        }
        
        // Mostrar si hay balance
        val pepsiBlackCount = detectionStats["Pepsi Black"] ?: 0
        val others = listOf("7up", "Mirinda", "Squirt").sumOf { detectionStats[it] ?: 0 }
        Log.d(TAG, "Pepsi Black: $pepsiBlackCount vs Otros objetivo: $others")
        Log.d(TAG, "Parámetros estabilidad: ${STABILITY_WINDOW_MS}ms, min ${MIN_CONSISTENT_DETECTIONS} detecciones")
        Log.d(TAG, "================================")
    }
    
    // Métodos para ajustar estabilidad si es necesario
    fun getStabilityInfo(): String {
        return "Ventana: ${STABILITY_WINDOW_MS}ms, Mín detecciones: $MIN_CONSISTENT_DETECTIONS"
    }
    
    // Método para diagnosticar el estado del detector
    fun getDiagnosticInfo(): String {
        return buildString {
            appendLine("=== YOLO DETECTOR DIAGNOSTICS ===")
            appendLine("Initialized: $isInitialized")
            appendLine("Interpreter: ${interpreter != null}")
            appendLine("Model file: $MODEL_FILENAME")
            appendLine("Labels loaded: ${labels.size} (${labels.joinToString(", ")})")
            appendLine("Confidence threshold: $CONFIDENCE_THRESHOLD")
            appendLine("Input size: $INPUT_SIZE")
            appendLine("Max detections: $MAX_DETECTION")
            appendLine("Current detection stats: $detectionStats")
            appendLine("Total detections: $totalDetections")
        }
    }
    
    // Método para generar detecciones fake para testing
    fun generateTestDetection(): List<TfLiteDetection> {
        Log.d(TAG, "🧪 Generando detección de prueba...")
        return listOf(
            TfLiteDetection(
                bbox = RectF(100f, 100f, 300f, 300f),
                confidence = 0.75f,
                classId = 4, // Pepsi
                className = "Pepsi"
            )
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
}