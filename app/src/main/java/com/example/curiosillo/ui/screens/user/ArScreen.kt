package com.example.curiosillo.ui.screens.user

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.screens.utils.coloreCategoria
import com.example.curiosillo.ui.screens.utils.emojiCategoria
import com.example.curiosillo.viewmodel.ArResult
import com.example.curiosillo.viewmodel.ArViewModel
import com.example.curiosillo.viewmodel.ArViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val viewModel: ArViewModel = viewModel(
        factory = ArViewModelFactory(app.repository, app.geminiPrefs, app.gamificationEngine)
    )
    val uiState by viewModel.uiState.collectAsState()

    val mostraPopupPref by app.contentPrefs.mostraPopupAR.collectAsState(initial = null)
    var showDialog by remember { mutableStateOf(false) }
    var nonMostrarePiu by remember { mutableStateOf(false) }

    // Buffer per preservare i dati durante le animazioni di uscita
    var bufferedResult by remember { mutableStateOf<ArResult?>(null) }
    var bufferedError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.result) {
        if (uiState.result != null) bufferedResult = uiState.result
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) bufferedError = uiState.error
    }

    // Gestione permessi camera
    var hasCameraPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    // Camera Provider State
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            delay(500)
            try {
                cameraProvider = ProcessCameraProvider.getInstance(ctx).await()
            } catch (e: Exception) {
                Log.e("ArScreen", "Errore recupero CameraProvider", e)
            }
        }
    }

    // Mostra il popup solo se la preferenza è caricata ed è true
    LaunchedEffect(mostraPopupPref) {
        if (mostraPopupPref == true) {
            showDialog = true
        }
    }

    // Dialog badge sbloccato
    uiState.badgeSbloccato?.let { badge ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissBadge() },
            icon = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Text(
                    "Badge sbloccato!",
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        badge.nome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        badge.descrizione, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissBadge() }) {
                    Text("Ottimo!")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission && cameraProvider != null) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(this.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider?.unbindAll()
                            cameraProvider?.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("ArScreen", "Errore bind camera", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!hasCameraPermission) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permesso fotocamera necessario", color = Color.White)
            }
        } else {
            // Caricamento camera
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Overlay UI
        Box(modifier = Modifier.fillMaxSize()) {
            
            // --- MIRINO CENTRALE ---
            if (hasCameraPermission && cameraProvider != null && !uiState.isLoading && uiState.result == null && uiState.error == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewInAr,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { nav.popBackStack() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                
                // Badge utilizzi rimanenti
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.Cyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (uiState.remainingUsages > 100) "∞" else uiState.remainingUsages.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { showDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Info, "Info", tint = Color.White)
                }
            }

            // Pulsante di scatto "GLASS"
            if (hasCameraPermission && cameraProvider != null && !uiState.isLoading && uiState.result == null && uiState.error == null) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = tween(100),
                    label = "shutterScale"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 60.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { 
                                    scattaEAnalizza(ctx, imageCapture, viewModel)
                                }
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }

            // Loading state
            if (uiState.isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Analisi in corso...", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Risultato (Fluid Exit)
            AnimatedVisibility(
                visible = uiState.result != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(600)) + fadeOut(animationSpec = tween(600)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                bufferedResult?.let { res ->
                    ArResultCard(result = res, onDismiss = { viewModel.resetState() })
                }
            }

            // Error display (Fluid Exit)
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(600)) + fadeOut(animationSpec = tween(600)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = if (uiState.remainingUsages == 0) "Limite raggiunto" else "Oops! Servizio non disponibile",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                IconButton(onClick = { viewModel.resetState() }) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (uiState.remainingUsages == 0) {
                                    "Hai esaurito i tuoi utilizzi giornalieri per la scansione AR. Torna domani per scoprire nuove curiosità!"
                                } else {
                                    "Al momento Curiosillo non può analizzare l'immagine. Assicurati di avere una connessione internet attiva o riprova tra qualche minuto.\n\nGrazie della comprensione."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetState() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Ho capito")
                            }
                        }
                    }
                }
            }
        }

        // Overlay per il popup informativo (Intro) - Background e Card animati separatamente per fluidità totale
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showDialog,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showDialog = false }
                )
            }
            
            AnimatedVisibility(
                visible = showDialog,
                enter = scaleIn(initialScale = 0.85f, animationSpec = tween(400, delayMillis = 100)) + fadeIn(animationSpec = tween(400, delayMillis = 100)),
                exit = scaleOut(targetScale = 0.85f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable(enabled = false) {}, 
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ViewInAr,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Esplora il Mondo in AR",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Con questa funzione puoi inquadrare oggetti, monumenti o elementi della natura per scoprire curiosità incredibili in tempo reale!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { nonMostrarePiu = !nonMostrarePiu }
                                .padding(4.dp)
                        ) {
                            Checkbox(
                                checked = nonMostrarePiu,
                                onCheckedChange = { nonMostrarePiu = it }
                            )
                            Text(
                                "Non mostrare di nuovo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (nonMostrarePiu) {
                                    scope.launch { app.contentPrefs.disabilitaPopupAR() }
                                }
                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Ho capito!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArResultCard(result: ArResult, onDismiss: () -> Unit) {
    val categoryColor = coloreCategoria(result.categoria)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${emojiCategoria(result.categoria)} ${result.categoria}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = categoryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = result.titolo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = result.curiosita,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scansiona un altro oggetto")
            }
        }
    }
}

private fun scattaEAnalizza(
    context: android.content.Context,
    imageCapture: ImageCapture,
    viewModel: ArViewModel
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                if (bitmap != null) {
                    viewModel.scanImage(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("ArScreen", "Errore scatto: ${exception.message}", exception)
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
