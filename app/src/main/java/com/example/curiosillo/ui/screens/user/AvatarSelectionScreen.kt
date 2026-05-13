package com.example.curiosillo.ui.screens.user

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.viewmodel.AvatarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen(
    nav: NavController,
    viewModel: AvatarViewModel
) {
    val avatarEquippato by viewModel.avatarEquippato.collectAsState()
    val avatarItems by viewModel.avatarItems.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            // Questo codice viene eseguito SOLO quando la schermata viene chiusa (es. l'utente torna alla Home)
            viewModel.onWardrobeClosed()
        }
    }

    // Google user data for the specific Google option
    val user = FirebaseManager.utenteCorrente
    val isGoogleUser = FirebaseManager.isGoogleUser()
    val photoUrl = user?.photoUrl?.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scegli il tuo Avatar") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Sblocca nuovi avatar salendo di livello!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Optimized grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Google Option if available
                if (isGoogleUser && photoUrl != null) {
                    item(key = "google_option") {
                        AvatarItem(
                            avatarId = "google",
                            isSbloccato = true,
                            isEquippato = avatarEquippato == "google",
                            isNew = false,
                            photoOverride = photoUrl,
                            labelOverride = "Google",
                            onAvatarClick = { viewModel.selezionaAvatar("google") }
                        )
                    }
                }

                // 2. Standard Catalog from ViewModel
                items(
                    items = avatarItems,
                    key = { it.id }
                ) { itemState ->
                    AvatarItem(
                        avatarId = itemState.id,
                        drawableRes = itemState.resourceId,
                        isSbloccato = itemState.isUnlocked,
                        isEquippato = itemState.isEquipped,
                        isNew = itemState.isNew,
                        livelloRichiesto = itemState.livelloRichiesto,
                        onAvatarClick = {
                            if (itemState.isUnlocked) viewModel.selezionaAvatar(itemState.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarItem(
    avatarId: String,
    @DrawableRes drawableRes: Int = 0,
    isSbloccato: Boolean,
    isEquippato: Boolean,
    isNew: Boolean = false,
    livelloRichiesto: Int = 1,
    photoOverride: String? = null,
    labelOverride: String? = null,
    onAvatarClick: () -> Unit
) {
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = if (isPressed) tween(100) else tween(50),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isSbloccato,
                onClick = onAvatarClick
            )
            .padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .then(
                    when {
                        isEquippato -> Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        // Golden border for new avatars
                        isNew -> Modifier.border(3.dp, Color(0xFFFFD700), CircleShape)
                        else -> Modifier
                    }
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isSbloccato) MaterialTheme.colorScheme.surfaceVariant else Color.LightGray)
        ) {
            if (photoOverride != null) {
                AsyncImage(
                    model = photoOverride,
                    contentDescription = avatarId,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (!isSbloccato) ColorFilter.colorMatrix(grayscaleMatrix) else null
                )
            } else {
                Image(
                    painter = painterResource(drawableRes),
                    contentDescription = avatarId,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (!isSbloccato) ColorFilter.colorMatrix(grayscaleMatrix) else null
                )
            }

            if (!isSbloccato) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Bloccato",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // "NEW" Badge overlay
            if (isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Red, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NOVITÀ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val textLabel = when {
            labelOverride != null -> if (isEquippato) "$labelOverride (Uso)" else labelOverride
            !isSbloccato -> "Liv. $livelloRichiesto"
            isEquippato -> "In uso"
            else -> "Sbloccato"
        }

        Text(
            textLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isEquippato) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.6f
            ),
            fontWeight = if (isEquippato) FontWeight.ExtraBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
