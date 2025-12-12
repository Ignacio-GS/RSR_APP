package com.example.rsrtest.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.rsrtest.data.DetectedProduct
import com.example.rsrtest.data.Store
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ScannerScreen(
    hasPermission: Boolean,
    selectedStore: Store?,
    detectedProducts: StateFlow<List<DetectedProduct>>,
    currentProduct: StateFlow<DetectedProduct?>,
    detectionCount: StateFlow<Int>,
    isCaptureMode: StateFlow<Boolean>,
    captureButtonText: StateFlow<String>,
    recentlyAddedProducts: StateFlow<Set<String>>,
    missingProducts: StateFlow<List<com.example.rsrtest.data.MissingProduct>>,
    currentContext: StateFlow<com.example.rsrtest.data.ShelfContext>,
    onRequestPermission: () -> Unit,
    onSaveDetections: (List<DetectedProduct>) -> Unit,
    onClearDetections: () -> Unit,
    onShareDetections: (List<DetectedProduct>) -> Unit,
    onResetAntiSpamSession: () -> Unit,
    onToggleCaptureMode: () -> Unit,
    onCaptureNow: () -> Unit,
    cameraContent: @Composable () -> Unit,
    headerSection: @Composable (Int, Store?) -> Unit,
    detectionStats: @Composable (List<DetectedProduct>) -> Unit,
    currentProductCard: @Composable (DetectedProduct) -> Unit,
    missingProductsCard: @Composable (List<com.example.rsrtest.data.MissingProduct>, com.example.rsrtest.data.ShelfContext) -> Unit,
    productHistoryCard: @Composable (List<DetectedProduct>, androidx.compose.foundation.lazy.LazyListState, Set<String>) -> Unit,
    floatingActionButtons: @Composable (onSave: () -> Unit, onClear: () -> Unit, onShare: () -> Unit) -> Unit,
    permissionScreen: @Composable (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Observar productos detectados para el scanner
    val products by detectedProducts.collectAsState()
    val currentHighlight by currentProduct.collectAsState()
    val totalDetections by detectionCount.collectAsState()
    
    // Observar estado de captura
    val isCaptureMode by isCaptureMode.collectAsState()
    val captureButtonText by captureButtonText.collectAsState()
    
    // Observar productos agregados recientemente
    val recentlyAdded by recentlyAddedProducts.collectAsState()
    
    // Observar productos faltantes
    val missingProducts by missingProducts.collectAsState()
    val currentContext by currentContext.collectAsState()

    // Para animaciones mejoradas
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Animaciones de entrada y transiciones
    val transition = rememberInfiniteTransition()
    val headerPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "headerPulse"
    )
    
    val detectionCountScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "detectionCountScale"
    )

    // Hacer scroll automático cuando se detecta un nuevo producto
    LaunchedEffect(products.size) {
        if (products.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    if (hasPermission) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Cámara de fondo
                cameraContent()

                // Overlay con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // UI Principal con animaciones mejoradas
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header con animación de pulso
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer(
                                scaleX = headerPulse,
                                scaleY = headerPulse
                            )
                        ) {
                            headerSection(totalDetections, selectedStore)
                        }
                    }

                    // Estadísticas con animación suave
                    AnimatedVisibility(
                        visible = products.size >= 3,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        detectionStats(products)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Producto destacado actual con animación de entrada
                    AnimatedVisibility(
                        visible = currentHighlight != null,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        currentHighlight?.let { product ->
                            currentProductCard(product)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sección de productos faltantes con transición mejorada
                    AnimatedVisibility(
                        visible = missingProducts.isNotEmpty(),
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(
                            animationSpec = tween(500)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it / 2 }
                        ) + fadeOut(
                            animationSpec = tween(300)
                        )
                    ) {
                        missingProductsCard(missingProducts, currentContext)
                    }
                    
                    if (missingProducts.isNotEmpty() && products.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Lista de productos detectados con animación escalonada
                    AnimatedVisibility(
                        visible = products.isNotEmpty(),
                        enter = slideInVertically(
                            initialOffsetY = { it }
                        ) + fadeIn(
                            animationSpec = tween(800)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it }
                        ) + fadeOut(
                            animationSpec = tween(400)
                        )
                    ) {
                        productHistoryCard(products, listState, recentlyAdded)
                    }
                }

                // Botones flotantes con animación de entrada escalonada
                AnimatedVisibility(
                    visible = products.isNotEmpty(),
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        floatingActionButtons(
                            {
                                onSaveDetections(products)
                                Toast.makeText(context, "¡Guardado en Firestore!", Toast.LENGTH_SHORT).show()
                            },
                            onClearDetections,
                            { onShareDetections(products) }
                        )
                    }
                }
                
                // Botones de captura en la parte inferior con animación
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.8f),
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.8f)
                                        )
                                    ),
                                    RoundedCornerShape(25.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        // Botón para resetear sesión
                        Button(
                            onClick = onResetAntiSpamSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                fontSize = 12.sp
                            )
                        }
                        
                        // Botón para cambiar modo
                        Button(
                            onClick = onToggleCaptureMode,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCaptureMode) Color.Red else Color.Blue
                            ),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (isCaptureMode) Icons.Default.CameraAlt else Icons.Default.AutoMode,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCaptureMode) "Manual" else "Auto",
                                fontSize = 14.sp
                            )
                        }
                        
                        // Botón de captura (solo visible en modo manual)
                        if (isCaptureMode) {
                            Button(
                                onClick = onCaptureNow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Green
                                ),
                                modifier = Modifier.size(50.dp)
                            ) {
                                if (captureButtonText == "⏳") {
                                    Text(
                                        text = "⏳",
                                        fontSize = 20.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        permissionScreen(onRequestPermission)
    }
}