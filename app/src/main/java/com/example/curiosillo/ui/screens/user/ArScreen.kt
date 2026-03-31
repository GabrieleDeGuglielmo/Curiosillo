package com.example.curiosillo.ui.screens.user

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val mostraPopupPref by app.contentPrefs.mostraPopupAR.collectAsState(initial = null)
    var showDialog by remember { mutableStateOf(false) }
    var nonMostrarePiu by remember { mutableStateOf(false) }

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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Rounded.ViewInAr, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
            title = {
                Text(
                    "Esplora il Mondo in AR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Con questa funzione puoi inquadrare oggetti, monumenti o elementi della natura per scoprire curiosità incredibili in tempo reale!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { nonMostrarePiu = !nonMostrarePiu }
                    ) {
                        Checkbox(
                            checked = nonMostrarePiu,
                            onCheckedChange = { nonMostrarePiu = it }
                        )
                        Text(
                            "Non mostrare di nuovo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nonMostrarePiu) {
                            scope.launch { app.contentPrefs.disabilitaPopupAR() }
                        }
                        showDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ho capito!")
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
            
            // --- MIRINO CENTRALE (SOLO L'ICONA DEL CUBO) ---
            if (hasCameraPermission && cameraProvider != null) {
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
                IconButton(
                    onClick = { showDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Info, "Info", tint = Color.White)
                }
            }

            // Pulsante di scatto "GLASS"
            if (hasCameraPermission && cameraProvider != null) {
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
                                onClick = { scattaFoto(ctx, imageCapture) }
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cerchio interno
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

private fun scattaFoto(context: android.content.Context, imageCapture: ImageCapture) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Curiosillo-AR")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        .build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("ArScreen", "Errore salvataggio foto: ${exc.message}", exc)
                Toast.makeText(context, "Errore durante lo scatto", Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Foto salvata con successo!", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
