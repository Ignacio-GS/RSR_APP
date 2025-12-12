package com.example.rsrtest.core.error

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor centralizado de errores con manejo de recuperación
 */
class ErrorHandler(private val context: Context) {
    
    private val _errors = MutableStateFlow<List<ErrorState>>(emptyList())
    val errors: StateFlow<List<ErrorState>> = _errors.asStateFlow()
    
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()
    
    sealed class ErrorState(
        val type: ErrorType,
        val message: String,
        val throwable: Throwable? = null,
        val isRecoverable: Boolean = false
    ) {
        object NetworkError : ErrorState(
            type = ErrorType.NETWORK,
            message = "Error de conexión. Verifica tu internet.",
            isRecoverable = true
        )
        
        object CameraError : ErrorState(
            type = ErrorType.CAMERA,
            message = "Error al acceder a la cámara.",
            isRecoverable = true
        )
        
        object MLError : ErrorState(
            type = ErrorType.ML_MODEL,
            message = "Error en el modelo de detección.",
            isRecoverable = true
        )
        
        object DatabaseError : ErrorState(
            type = ErrorType.DATABASE,
            message = "Error en la base de datos local.",
            isRecoverable = true
        )
        
        object FirebaseError : ErrorState(
            type = ErrorType.FIREBASE,
            message = "Error al sincronizar con la nube.",
            isRecoverable = true
        )
        
        class PermissionError(val permission: String) : ErrorState(
            type = ErrorType.PERMISSION,
            message = "Permiso denegado: $permission",
            isRecoverable = true
        )
        
        class UnknownError(throwable: Throwable) : ErrorState(
            type = ErrorType.UNKNOWN,
            message = "Error inesperado: ${throwable.message}",
            throwable = throwable,
            isRecoverable = false
        )
    }
    
    enum class ErrorType {
        NETWORK, CAMERA, ML_MODEL, DATABASE, FIREBASE, PERMISSION, UNKNOWN
    }
    
    fun handleError(error: Throwable, context: String = ""): ErrorState {
        val errorState = when (error) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException -> {
                Log.e("ErrorHandler", "Network error in $context", error)
                ErrorState.NetworkError
            }
            is java.lang.SecurityException -> {
                Log.e("ErrorHandler", "Permission error in $context", error)
                ErrorState.PermissionError("Camera/Location")
            }
            is Exception -> {
                when {
                    error.message?.contains("Room", ignoreCase = true) == true ||
                    error.stackTrace.any { it.className.contains("androidx.room") } -> {
                        Log.e("ErrorHandler", "Database error in $context", error)
                        ErrorState.DatabaseError
                    }
                    else -> {
                        Log.e("ErrorHandler", "Unknown error in $context", error)
                        ErrorState.UnknownError(error)
                    }
                }
            }
            is com.google.firebase.FirebaseException -> {
                Log.e("ErrorHandler", "Firebase error in $context", error)
                ErrorState.FirebaseError
            }
            else -> {
                Log.e("ErrorHandler", "Unknown error in $context", error)
                ErrorState.UnknownError(error)
            }
        }
        
        addError(errorState)
        return errorState
    }
    
    fun handleCustomError(errorState: ErrorState) {
        addError(errorState)
    }
    
    private fun addError(errorState: ErrorState) {
        _errors.value = _errors.value + errorState
        
        // Auto-limpiar errores después de 5 segundos si son recuperables
        if (errorState.isRecoverable) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                removeError(errorState)
            }, 5000)
        }
    }
    
    fun removeError(errorState: ErrorState) {
        _errors.value = _errors.value - errorState
    }
    
    fun clearErrors() {
        _errors.value = emptyList()
    }
    
    suspend fun <T> withErrorHandling(
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            val result = block()
            Result.success(result)
        } catch (e: Exception) {
            handleError(e, operation)
            Result.failure(e)
        }
    }
    
    suspend fun <T> retryOperation(
        operation: String,
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        block: suspend () -> T
    ): Result<T> {
        var lastError: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = block()
                return Result.success(result)
            } catch (e: Exception) {
                lastError = e
                Log.w("ErrorHandler", "Attempt $attempt failed for $operation: ${e.message}")
                
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(delayMs * (attempt + 1))
                }
            }
        }
        
        lastError?.let { handleError(it, operation) }
        return Result.failure(lastError ?: Exception("Unknown error"))
    }
    
    fun isRecoverable(errorState: ErrorState): Boolean {
        return errorState.isRecoverable
    }
    
    suspend fun attemptRecovery(errorState: ErrorState): Boolean {
        _isRecovering.value = true
        
        return try {
            val success = when (errorState) {
                is ErrorState.NetworkError -> recoverNetworkError()
                is ErrorState.CameraError -> recoverCameraError()
                is ErrorState.MLError -> recoverMLError()
                is ErrorState.DatabaseError -> recoverDatabaseError()
                is ErrorState.FirebaseError -> recoverFirebaseError()
                is ErrorState.PermissionError -> recoverPermissionError(errorState.permission)
                else -> false
            }
            
            if (success) {
                removeError(errorState)
            }
            
            success
        } catch (e: Exception) {
            Log.e("ErrorHandler", "Recovery failed for ${errorState.type}", e)
            false
        } finally {
            _isRecovering.value = false
        }
    }
    
    private suspend fun recoverNetworkError(): Boolean {
        // Implementar lógica de recuperación de red
        return true
    }
    
    private suspend fun recoverCameraError(): Boolean {
        // Implementar lógica de recuperación de cámara
        return true
    }
    
    private suspend fun recoverMLError(): Boolean {
        // Implementar lógica de recuperación de ML
        return true
    }
    
    private suspend fun recoverDatabaseError(): Boolean {
        // Implementar lógica de recuperación de base de datos
        return true
    }
    
    private suspend fun recoverFirebaseError(): Boolean {
        // Implementar lógica de recuperación de Firebase
        return true
    }
    
    private suspend fun recoverPermissionError(permission: String): Boolean {
        // Implementar lógica de recuperación de permisos
        return true
    }
}