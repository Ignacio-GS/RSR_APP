package com.example.rsrtest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rsrtest.ui.theme.RSRTESTTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Data class para productos detectados
data class DetectedProduct(
    val name: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
)

// Elemento del carrito de compras
data class CartItem(
    val name: String,
    val quantity: Int = 1
)

// Colores del tema PepsiCo
object PepsiColors {
    val PepsiBlue = Color(0xFF004B93)
    val PepsiRed = Color(0xFFE32934)
    val PepsiWhite = Color(0xFFFFFFFF)
    val DarkBlue = Color(0xFF002F6C)
    val LightBlue = Color(0xFF0065BD)
    val BackgroundDark = Color(0xFF0A1929)
    val CardDark = Color(0xFF1E3A5F)
    val AccentOrange = Color(0xFFFF6B35)
    val SuccessGreen = Color(0xFF4CAF50)
}

// ===== FUNCIONES AUXILIARES =====

fun vibrateOnDetection(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}

fun shareDetections(context: Context, products: List<DetectedProduct>) {
    val shareText = buildString {
        appendLine("🔍 Productos PepsiCo Detectados:")
        appendLine()

        val groupedProducts = products.groupBy { it.name }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        groupedProducts.forEach { (product, count) ->
            appendLine("• $product: $count vez(veces)")
        }

        appendLine()
        appendLine("Total: ${products.size} detecciones")
        appendLine("📱 Escaneado con PepsiCo Scanner")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Compartir detecciones")
    context.startActivity(shareIntent)
}

class MainActivity : ComponentActivity() {

    // Firestore
    private lateinit var db: FirebaseFirestore

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Camera Executor
    private lateinit var cameraExecutor: ExecutorService

    // ML Kit Object Detector
    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    // Estado para productos detectados
    private val _detectedProducts = MutableStateFlow<List<DetectedProduct>>(emptyList())
    val detectedProducts: StateFlow<List<DetectedProduct>> = _detectedProducts.asStateFlow()

    // Estado para el producto actual destacado
    private val _currentProduct = MutableStateFlow<DetectedProduct?>(null)
    val currentProduct: StateFlow<DetectedProduct?> = _currentProduct.asStateFlow()

    // Contador de detecciones
    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()

    // Carrito de compras
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private fun addToCart(productName: String) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == productName }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(productName, 1))
        }
        _cartItems.value = current
    }

    private fun updateCartItem(productName: String, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.name == productName }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty > 0) {
                current[index] = item.copy(quantity = newQty)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }
    }

    private fun removeFromCart(productName: String) {
        val current = _cartItems.value.toMutableList()
        current.removeAll { it.name == productName }
        _cartItems.value = current
    }

    // Estado del permiso de cámara
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    // Lista de productos PepsiCo con categorías
    private val pepsicoProducts = mapOf(
        // Bebidas
        "pepsi" to "Pepsi",
        "coca cola" to "Bebida Cola",
        "bottle" to "Bebida Embotellada",
        "can" to "Bebida Enlatada",
        "soda" to "Refresco",
        "beverage" to "Bebida",
        "7up" to "7UP",
        "mirinda" to "Mirinda",
        "mountain dew" to "Mountain Dew",
        "gatorade" to "Gatorade",
        "tropicana" to "Tropicana",
        "aquafina" to "Aquafina",
        "lipton" to "Lipton",
        "h2oh" to "H2OH!",
        "manzanita" to "Manzanita Sol",

        // Snacks
        "chips" to "Papas Fritas",
        "doritos" to "Doritos",
        "cheetos" to "Cheetos",
        "lays" to "Lay's",
        "ruffles" to "Ruffles",
        "sabritas" to "Sabritas",
        "fritos" to "Fritos",
        "tostitos" to "Tostitos",
        "takis" to "Takis",
        "chip bag" to "Bolsa de Papas",
        "snack" to "Botana",

        // Alimentos
        "quaker" to "Quaker",
        "oats" to "Avena Quaker",
        "cereal" to "Cereal",
        "gamesa" to "Gamesa",
        "emperador" to "Emperador",

        // Términos generales
        "bag" to "Producto Empaquetado",
        "package" to "Producto PepsiCo",
        "food" to "Alimento",
        "drink" to "Bebida",
        "snack food" to "Botana"
    )

    // Launcher para permisos de cámara
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        _hasCameraPermission.value = isGranted
        if (!isGranted) {
            Toast.makeText(
                this,
                "Se necesita el permiso de cámara para escanear productos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar servicios
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Verificar permiso de cámara inicial
        checkCameraPermission()

        // Prueba de Firestore
        testFirestore()

        // Verificar y solicitar ubicación
        checkLocationPermission()

        // Configurar UI
        setContent {
            RSRTESTTheme {
                MainScreen()
            }
        }
    }

    private fun checkCameraPermission() {
        _hasCameraPermission.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                _hasCameraPermission.value = true
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Mostrar explicación adicional
                Toast.makeText(
                    this,
                    "La cámara es necesaria para detectar productos PepsiCo",
                    Toast.LENGTH_LONG
                ).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun testFirestore() {
        db.collection("orders")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshots ->
                Log.d("Firestore", "Documentos encontrados: ${snapshots.size()}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al leer orders", e)
            }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLastLocation()
            }
            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getLastLocation()
        } else {
            Log.w("Location", "Permiso de ubicación denegado")
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        Log.d("Location", "Ubicación: ${it.latitude}, ${it.longitude}")
                    } ?: Log.d("Location", "Ubicación no disponible")
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error obteniendo ubicación", e)
                }
        }
    }

    private fun saveDetectionsToFirestore(
        products: List<DetectedProduct>,
        location: Location?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val batch = db.batch()

        val sessionData = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "location" to if (location != null) {
                hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                )
            } else null,
            "totalDetections" to products.size,
            "products" to products.map { product ->
                hashMapOf(
                    "name" to product.name,
                    "confidence" to product.confidence,
                    "timestamp" to product.timestamp
                )
            }
        )

        val docRef = db.collection("detection_sessions").document()
        batch.set(docRef, sessionData)

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    @Composable
    fun MainScreen() {
        // Observar el estado del permiso
        val hasPermission by hasCameraPermission.collectAsState()

        // Observar productos detectados
        val products by detectedProducts.collectAsState()
        val currentHighlight by currentProduct.collectAsState()
        val totalDetections by detectionCount.collectAsState()
        val cartItemsList by cartItems.collectAsState()

        // Para animaciones
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        var showCart by remember { mutableStateOf(false) }

        // Hacer scroll automático cuando se detecta un nuevo producto
        LaunchedEffect(products.size) {
            if (products.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }

        if (showCart) {
            CartScreen(
                items = cartItemsList,
                onIncrement = { updateCartItem(it.name, 1) },
                onDecrement = { updateCartItem(it.name, -1) },
                onRemove = { removeFromCart(it.name) },
                onBack = { showCart = false }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                when {
                    hasPermission -> {
                        // Cámara de fondo
                        CameraScreen()

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

                        // UI Principal
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Header animado
                            HeaderSection(totalDetections)

                            // Estadísticas si hay productos
                            if (products.size >= 3) {
                                DetectionStats(products)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Producto destacado actual
                            currentHighlight?.let { product ->
                                CurrentProductCard(product)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Lista de productos detectados
                            AnimatedVisibility(
                                visible = products.isNotEmpty(),
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                ProductHistoryCard(products, listState)
                            }
                        }

                        // Botones flotantes
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            FloatingActionButtons(
                                onSaveDetections = {
                                    saveDetectionsToFirestore(
                                        products = products,
                                        location = null, // Aquí podrías pasar la ubicación real
                                        onSuccess = {
                                            Toast.makeText(this@MainActivity, "¡Guardado en Firestore!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { e ->
                                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onClearDetections = {
                                    _detectedProducts.value = emptyList()
                                    _detectionCount.value = 0
                                    _currentProduct.value = null
                                    _cartItems.value = emptyList()
                                },
                                onShareDetections = {
                                    shareDetections(this@MainActivity, products)
                                },
                                onShowCart = { showCart = true }
                            )
                        }
                    }
                    else -> {
                        // Pantalla de permiso
                        PermissionScreen(onRequestPermission = {
                            requestCameraPermission()
                        })
                    }
                }
            }
        }
    }

    @Composable
    fun HeaderSection(detectionCount: Int) {
        val infiniteTransition = rememberInfiniteTransition()
        val animatedColor by infiniteTransition.animateColor(
            initialValue = PepsiColors.PepsiBlue,
            targetValue = PepsiColors.LightBlue,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.BackgroundDark.copy(alpha = 0.9f)
            ),
            border = BorderStroke(2.dp, animatedColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PepsiCo Scanner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Detectando productos...",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Contador animado
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(animatedColor)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = detectionCount.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    fun FloatingActionButtons(
        onSaveDetections: () -> Unit,
        onClearDetections: () -> Unit,
        onShareDetections: () -> Unit,
        onShowCart: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Botón de carrito
            FloatingActionButton(
                onClick = onShowCart,
                containerColor = PepsiColors.PepsiBlue,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Carrito",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón de compartir
            FloatingActionButton(
                onClick = onShareDetections,
                containerColor = PepsiColors.LightBlue,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón de guardar
            FloatingActionButton(
                onClick = onSaveDetections,
                containerColor = PepsiColors.SuccessGreen,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Guardar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón de limpiar
            FloatingActionButton(
                onClick = onClearDetections,
                containerColor = PepsiColors.PepsiRed,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Limpiar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    @Composable
    fun CartScreen(
        items: List<CartItem>,
        onIncrement: (CartItem) -> Unit,
        onDecrement: (CartItem) -> Unit,
        onRemove: (CartItem) -> Unit,
        onBack: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Carrito de Compras", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PepsiColors.PepsiBlue)
                )
            },
            containerColor = PepsiColors.BackgroundDark
        ) { padding ->
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos en el carrito", color = Color.White)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.name }) { item ->
                        CartItemRow(item, onIncrement, onDecrement, onRemove)
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
                colors = CardDefaults.cardColors(
                    containerColor = PepsiColors.CardDark.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
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
                // Ícono animado
                val rotation by rememberInfiniteTransition().animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing)
                    )
                )

                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = PepsiColors.AccentOrange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre del producto
                Text(
                    text = product.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de confianza animada
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.value)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        PepsiColors.SuccessGreen,
                                        PepsiColors.AccentOrange
                                    )
                                )
                            )
                    )

                    Text(
                        text = "${(product.confidence * 100).toInt()}%",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timestamp
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(product.timestamp)),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }

    @Composable
    fun ProductHistoryCard(
        products: List<DetectedProduct>,
        listState: LazyListState
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = PepsiColors.BackgroundDark.copy(alpha = 0.95f)
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
                        text = "Historial de Detecciones",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = PepsiColors.LightBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = products.take(10),
                        key = { it.id }
                    ) { product ->
                        ProductHistoryItem(product)
                    }
                }
            }
        }
    }

    @Composable
    fun ProductHistoryItem(product: DetectedProduct) {
        val animatedAlpha = remember { Animatable(0f) }

        LaunchedEffect(product) {
            animatedAlpha.animateTo(1f, animationSpec = tween(500))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = animatedAlpha.value }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PepsiColors.PepsiBlue.copy(alpha = 0.3f),
                            PepsiColors.PepsiBlue.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PepsiColors.AccentOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(product.confidence * 100).toInt()}%",
                    color = PepsiColors.SuccessGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(product.timestamp)),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    fun CartItemRow(
        item: CartItem,
        onIncrement: (CartItem) -> Unit,
        onDecrement: (CartItem) -> Unit,
        onRemove: (CartItem) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PepsiColors.PepsiBlue.copy(alpha = 0.3f),
                            PepsiColors.PepsiBlue.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDecrement(item) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Disminuir", tint = Color.White)
                }
                Text(
                    text = item.quantity.toString(),
                    color = Color.White,
                    fontSize = 14.sp
                )
                IconButton(onClick = { onIncrement(item) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = Color.White)
                }
            }

            IconButton(onClick = { onRemove(item) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = PepsiColors.PepsiRed)
            }
        }
    }

    @Composable
    fun PermissionScreen(onRequestPermission: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PepsiColors.DarkBlue,
                            PepsiColors.BackgroundDark
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PepsiColors.CardDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = PepsiColors.PepsiRed
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Permiso de Cámara",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Para detectar productos PepsiCo necesitamos acceso a tu cámara",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PepsiColors.PepsiRed
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permitir Acceso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CameraScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(context) }

        DisposableEffect(previewView) {
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Image Analysis
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Log.e("CameraX", "Error vinculando casos de uso", e)
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                // Limpieza si es necesaria
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }

    @androidx.camera.core.ExperimentalGetImage
    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        override fun analyze(imageProxy: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            // Analizar solo cada 500ms para no saturar
            if (currentTimestamp - lastAnalyzedTimestamp >= 500) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    objectDetector.process(image)
                        .addOnSuccessListener { objects ->
                            val pepsicoDetections = mutableListOf<DetectedProduct>()

                            for (detectedObject in objects) {
                                for (label in detectedObject.labels) {
                                    val labelText = label.text.lowercase()

                                    // Verificar si es un producto PepsiCo
                                    val matchedProduct = pepsicoProducts.entries.find { (key, _) ->
                                        labelText.contains(key) || key.contains(labelText)
                                    }

                                    matchedProduct?.let { (_, productName) ->
                                        val product = DetectedProduct(
                                            name = productName,
                                            confidence = label.confidence
                                        )
                                        pepsicoDetections.add(product)
                                        addToCart(productName)

                                        // Actualizar producto actual si tiene alta confianza
                                        if (label.confidence > 0.7f) {
                                            _currentProduct.value = product
                                        }

                                        Log.d("PepsiCo", "Detectado: $productName (${label.confidence})")
                                    }
                                }
                            }

                            // Actualizar la lista de productos detectados
                            if (pepsicoDetections.isNotEmpty()) {
                                val currentList = _detectedProducts.value.toMutableList()
                                currentList.addAll(0, pepsicoDetections)
                                _detectedProducts.value = currentList.take(50) // Mantener máximo 50
                                _detectionCount.value += pepsicoDetections.size

                                // Vibrar al detectar
                                vibrateOnDetection(this@MainActivity)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MLKit", "Error en detección", e)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }

                    lastAnalyzedTimestamp = currentTimestamp
                } else {
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        objectDetector.close()
    }
}