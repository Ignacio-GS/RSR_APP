package com.example.rsrtest.ui.components

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.rsrtest.data.DetectedProduct
import com.example.rsrtest.data.Store
import com.example.rsrtest.data.MissingProduct
import com.example.rsrtest.data.ShelfContext
import com.example.rsrtest.ml.CameraMLManager
import java.util.concurrent.ExecutorService
// Define PepsiColors if not available in theme
object PepsiColors {
    val PepsiBlue = Color(0xFF004B8D)
    val LightBlue = Color(0xFF0066CC)
    val PepsiRed = Color(0xFFE32636)
    val AccentOrange = Color(0xFFFF6B35)
    val CardDark = Color(0xFF1A1A1A)
}

@Composable
fun HeaderSection(detectionCount: Int, selectedStore: Store? = null) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedColor by infiniteTransition.animateColor(
        initialValue = PepsiColors.PepsiBlue,
        targetValue = PepsiColors.LightBlue,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark
        ),
        border = BorderStroke(1.dp, animatedColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RSR Scanner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Detección en tiempo real",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            animatedColor,
                            CircleShape
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "$detectionCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            selectedStore?.let { store ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PepsiColors.PepsiBlue.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = store.customerName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${store.chainLevel1} - ${store.chainLevel2}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionStats(products: List<DetectedProduct>) {
    val productCounts = products.groupBy { it.name }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    if (productCounts.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Productos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = PepsiColors.AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                productCounts.forEach { (product, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = product,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    PepsiColors.AccentOrange,
                                    CircleShape
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentProductCard(product: DetectedProduct) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(product) {
        animatedProgress.animateTo(
            targetValue = product.confidence,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark
        ),
        border = BorderStroke(
            2.dp,
            Brush.linearGradient(
                colors = listOf(
                    PepsiColors.PepsiRed,
                    PepsiColors.PepsiBlue
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rotation by rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = ""
            )

            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = PepsiColors.AccentOrange,
                modifier = Modifier
                    .size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DETECTADO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PepsiColors.AccentOrange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confianza:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Text(
                    text = "${(product.confidence * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        product.confidence >= 0.8f -> Color.Green
                        product.confidence >= 0.6f -> PepsiColors.AccentOrange
                        else -> Color.Red
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    product.confidence >= 0.8f -> Color.Green
                    product.confidence >= 0.6f -> PepsiColors.AccentOrange
                    else -> Color.Red
                },
                trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
fun MissingProductsCard(
    missingProducts: List<MissingProduct>,
    currentContext: ShelfContext,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark.copy(alpha = 0.95f)
        ),
        border = BorderStroke(1.dp, PepsiColors.PepsiRed.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = PepsiColors.PepsiRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Productos Faltantes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(PepsiColors.PepsiRed.copy(alpha = 0.3f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = missingProducts.size.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contexto actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = PepsiColors.LightBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Contexto: ${currentContext.displayName}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(missingProducts) { missingProduct ->
                    MissingProductItem(missingProduct, pulseScale)
                }
            }
        }
    }
}

@Composable
fun MissingProductItem(
    missingProduct: MissingProduct,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.PepsiRed.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = PepsiColors.PepsiRed,
                modifier = Modifier
                    .size(24.dp)
                    .scale(pulseScale)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = missingProduct.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = missingProduct.reason,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = missingProduct.category,
                        color = PepsiColors.LightBlue.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    
                    Text(
                        text = "${(missingProduct.confidence * 100).toInt()}%",
                        color = when {
                            missingProduct.confidence >= 0.8f -> Color.Red
                            missingProduct.confidence >= 0.6f -> PepsiColors.AccentOrange
                            else -> Color.Yellow
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProductHistoryCard(
    products: List<DetectedProduct>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    recentlyAdded: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, PepsiColors.LightBlue.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PepsiColors.LightBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historial de Detecciones",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "${products.size} productos",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (products.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products.reversed()) { product ->
                        ProductHistoryItem(product, recentlyAdded.contains(product.name.lowercase()))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay detecciones aún",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductHistoryItem(
    product: DetectedProduct,
    isRecentlyAdded: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecentlyAdded) {
                PepsiColors.AccentOrange.copy(alpha = glowAlpha)
            } else {
                PepsiColors.CardDark.copy(alpha = 0.7f)
            }
        ),
        border = if (isRecentlyAdded) {
            BorderStroke(1.dp, PepsiColors.AccentOrange)
        } else {
            BorderStroke(1.dp, Color.Transparent)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRecentlyAdded) Icons.Default.NewReleases else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isRecentlyAdded) PepsiColors.AccentOrange else Color.Green,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(product.confidence * 100).toInt()}% confianza",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(product.timestamp)),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingActionButtons(
    onSave: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Botón de guardar
        FloatingActionButton(
            onClick = onSave,
            containerColor = PepsiColors.PepsiBlue,
            contentColor = Color.White,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Guardar",
                modifier = Modifier.size(20.dp)
            )
        }

        // Botón de limpiar
        FloatingActionButton(
            onClick = onClear,
            containerColor = PepsiColors.PepsiRed,
            contentColor = Color.White,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Limpiar",
                modifier = Modifier.size(20.dp)
            )
        }

        // Botón de compartir
        FloatingActionButton(
            onClick = onShare,
            containerColor = PepsiColors.AccentOrange,
            contentColor = Color.White,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Compartir",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CameraPreview(
    cameraMLManager: CameraMLManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // Preview use case
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                // Image analysis use case
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraMLManager.getCameraExecutor(), cameraMLManager.getImageAnalyzer())
                    }
                
                // Camera selector
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()
                    
                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    
                } catch (exc: Exception) {
                    android.util.Log.e("CameraPreview", "Use case binding failed", exc)
                }
                
            }, ContextCompat.getMainExecutor(context))
        }
    )
}