package com.example.rsrtest.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rsrtest.core.error.ErrorHandler
import com.example.rsrtest.core.offline.OfflineManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Componente para mostrar errores globales
 */
@Composable
fun ErrorDisplay(
    errors: StateFlow<List<ErrorHandler.ErrorState>>,
    onRetry: (ErrorHandler.ErrorState) -> Unit = {},
    onDismiss: (ErrorHandler.ErrorState) -> Unit = {}
) {
    val currentErrors by errors.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        currentErrors.forEach { error ->
            ErrorItem(
                error = error,
                onRetry = { onRetry(error) },
                onDismiss = { onDismiss(error) }
            )
            
            if (error != currentErrors.last()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ErrorItem(
    error: ErrorHandler.ErrorState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (error.type) {
                ErrorHandler.ErrorType.NETWORK -> Color(0xFFFFF3E0)
                ErrorHandler.ErrorType.CAMERA -> Color(0xFFFFEBEE)
                ErrorHandler.ErrorType.ML_MODEL -> Color(0xFFF3E5F5)
                ErrorHandler.ErrorType.DATABASE -> Color(0xFFE8F5E8)
                ErrorHandler.ErrorType.FIREBASE -> Color(0xFFE3F2FD)
                ErrorHandler.ErrorType.PERMISSION -> Color(0xFFFFF8E1)
                ErrorHandler.ErrorType.UNKNOWN -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (error.type) {
                    ErrorHandler.ErrorType.NETWORK -> Icons.Default.WifiOff
                    ErrorHandler.ErrorType.CAMERA -> Icons.Default.NoPhotography
                    ErrorHandler.ErrorType.ML_MODEL -> Icons.Default.Memory
                    ErrorHandler.ErrorType.DATABASE -> Icons.Default.Storage
                    ErrorHandler.ErrorType.FIREBASE -> Icons.Default.CloudOff
                    ErrorHandler.ErrorType.PERMISSION -> Icons.Default.NoAccounts
                    ErrorHandler.ErrorType.UNKNOWN -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (error.type) {
                    ErrorHandler.ErrorType.NETWORK -> Color(0xFFE65100)
                    ErrorHandler.ErrorType.CAMERA -> Color(0xFFC62828)
                    ErrorHandler.ErrorType.ML_MODEL -> Color(0xFF6A1B9A)
                    ErrorHandler.ErrorType.DATABASE -> Color(0xFF2E7D32)
                    ErrorHandler.ErrorType.FIREBASE -> Color(0xFF1565C0)
                    ErrorHandler.ErrorType.PERMISSION -> Color(0xFFEF6C00)
                    ErrorHandler.ErrorType.UNKNOWN -> Color(0xFFC62828)
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (error.type) {
                        ErrorHandler.ErrorType.NETWORK -> "Error de Red"
                        ErrorHandler.ErrorType.CAMERA -> "Error de Cámara"
                        ErrorHandler.ErrorType.ML_MODEL -> "Error de Detección"
                        ErrorHandler.ErrorType.DATABASE -> "Error de Base de Datos"
                        ErrorHandler.ErrorType.FIREBASE -> "Error de Sincronización"
                        ErrorHandler.ErrorType.PERMISSION -> "Error de Permisos"
                        ErrorHandler.ErrorType.UNKNOWN -> "Error Desconocido"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            
            if (error.isRecoverable) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reintentar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Componente para mostrar estado offline
 */
@Composable
fun OfflineStatusBar(
    isOfflineMode: StateFlow<Boolean>,
    pendingSyncItems: StateFlow<List<OfflineManager.SyncItem>>,
    onForceSync: () -> Unit = {}
) {
    val isOffline by isOfflineMode.collectAsState()
    val pendingItems by pendingSyncItems.collectAsState()
    
    AnimatedVisibility(
        visible = isOffline || pendingItems.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isOffline) Color(0xFFFF6B6B) else Color(0xFFFFA726),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Sync,
                    contentDescription = null,
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isOffline) "Modo Offline" else "Sincronización Pendiente",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (pendingItems.isNotEmpty()) {
                        Text(
                            text = "${pendingItems.size} items pendientes",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (!isOffline && pendingItems.isNotEmpty()) {
                    IconButton(
                        onClick = onForceSync,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar ahora",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente para mostrar progreso de sincronización
 */
@Composable
fun SyncProgressDialog(
    syncProgress: StateFlow<OfflineManager.SyncProgress?>,
    onCancel: () -> Unit = {}
) {
    val progress by syncProgress.collectAsState()
    
    progress?.let { currentProgress ->
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text("Sincronizando")
            },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = currentProgress.processedItems.toFloat() / currentProgress.totalItems.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = currentProgress.currentOperation,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${currentProgress.processedItems}/${currentProgress.totalItems} completado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
        )
    }
}