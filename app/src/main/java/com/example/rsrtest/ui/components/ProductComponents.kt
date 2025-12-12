package com.example.rsrtest.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rsrtest.data.*
import java.text.NumberFormat
import java.util.*

// Componentes para ProductsScreen
@Composable
fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(shimmerScale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark
        ),
        border = BorderStroke(1.dp, PepsiColors.LightBlue.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PepsiColors.PepsiBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = PepsiColors.LightBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del producto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.brand,
                    color = PepsiColors.LightBlue.copy(alpha = 0.8f),
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
                        text = formatCurrency(product.price, product.currency),
                        color = PepsiColors.AccentOrange,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PepsiColors.PepsiBlue.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = product.category,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Botón de agregar
            FloatingActionButton(
                onClick = onAddToCart,
                containerColor = PepsiColors.AccentOrange,
                contentColor = Color.White,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar al carrito",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Componentes para CartScreen
@Composable
fun CartItemRow(
    cartItem: CartItemWithProduct,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark.copy(alpha = pulseAlpha)
        ),
        border = BorderStroke(1.dp, PepsiColors.AccentOrange.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PepsiColors.PepsiBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (cartItem.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cartItem.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = cartItem.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = PepsiColors.LightBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del producto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = cartItem.brand,
                    color = PepsiColors.LightBlue.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatCurrency(cartItem.price, cartItem.currency),
                    color = PepsiColors.AccentOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Controles de cantidad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.quantity - 1) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PepsiColors.PepsiRed.copy(alpha = 0.7f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Reducir cantidad",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = cartItem.quantity.toString(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )

                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.quantity + 1) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = PepsiColors.AccentOrange.copy(alpha = 0.7f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar cantidad",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Total: ${formatCurrency(cartItem.price * cartItem.quantity, cartItem.currency)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de eliminar
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PepsiColors.PepsiRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar del carrito",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Componentes para HistoryScreen
@Composable
fun PurchaseHistoryCard(
    purchase: PurchaseHistory,
    onPurchaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
      // Simplified animation for build compatibility
    val borderGlow = PepsiColors.PepsiBlue.copy(alpha = 0.3f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PepsiColors.CardDark
        ),
        border = BorderStroke(1.dp, borderGlow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onPurchaseClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = PepsiColors.AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Compra #${purchase.id.take(8)}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatDate(purchase.purchaseDate),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (purchase.status) {
                        "completed" -> Color.Green.copy(alpha = 0.3f)
                        "pending" -> PepsiColors.AccentOrange.copy(alpha = 0.3f)
                        "cancelled" -> PepsiColors.PepsiRed.copy(alpha = 0.3f)
                        else -> PepsiColors.PepsiBlue.copy(alpha = 0.3f)
                    }
                ) {
                    Text(
                        text = purchase.status.uppercase(),
                        color = when (purchase.status) {
                            "completed" -> Color.Green
                            "pending" -> PepsiColors.AccentOrange
                            "cancelled" -> PepsiColors.PepsiRed
                            else -> PepsiColors.LightBlue
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${purchase.itemCount} productos",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatCurrency(purchase.totalAmount, "MXN"),
                        color = PepsiColors.AccentOrange,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Ver detalles",
                    tint = PepsiColors.LightBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Componentes para StoresScreen
@Composable
fun StoreCard(
    store: Store,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val selectionScale by infiniteTransition.animateFloat(
        initialValue = if (isSelected) 1.05f else 1f,
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(selectionScale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                PepsiColors.PepsiBlue.copy(alpha = 0.4f)
            } else {
                PepsiColors.CardDark
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, PepsiColors.AccentOrange)
        } else {
            BorderStroke(1.dp, PepsiColors.LightBlue.copy(alpha = 0.3f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = store.customerName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = store.chainLevel1,
                        color = PepsiColors.LightBlue.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = store.chainLevel2,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PepsiColors.PepsiBlue.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "ID: ${store.customerNumber}",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Seleccionada",
                        tint = PepsiColors.AccentOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Funciones de utilidad
@Composable
fun formatCurrency(amount: Double, currency: String = "MXN"): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    format.currency = java.util.Currency.getInstance(currency)
    return format.format(amount)
}

@Composable
fun formatDate(timestamp: Long): String {
    return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(java.util.Date(timestamp))
}