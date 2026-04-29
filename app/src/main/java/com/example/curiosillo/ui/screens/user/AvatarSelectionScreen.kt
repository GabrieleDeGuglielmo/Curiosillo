package com.example.curiosillo.ui.screens.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.curiosillo.data.Avatar
import com.example.curiosillo.data.AvatarCatalogo
import com.example.curiosillo.domain.LivelloHelper
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.viewmodel.AvatarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen(
    nav: NavController,
    viewModel: AvatarViewModel
) {
    val avatarEquippato by viewModel.avatarEquippato.collectAsState()
    val xpTotali by viewModel.xpTotali.collectAsState()

    // Calcoliamo il livello una sola volta per la schermata
    val livelloAttuale = remember(xpTotali) { LivelloHelper.daXp(xpTotali).numero }

    // Dati dell'utente per l'opzione Google
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

            // Griglia ottimizzata
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Opzione Google se disponibile
                if (isGoogleUser && photoUrl != null) {
                    item(key = "google_option") {
                        AvatarItem(
                            avatarId = "google",
                            isSbloccato = true,
                            isEquippato = avatarEquippato == "google",
                            photoOverride = photoUrl,
                            labelOverride = "Google",
                            onAvatarClick = { viewModel.selezionaAvatar("google") }
                        )
                    }
                }

                // 2. Catalogo standard
                items(
                    items = AvatarCatalogo.lista,
                    key = { it.id }
                ) { avatar ->
                    val isSbloccato = livelloAttuale >= avatar.livelloRichiesto
                    val isEquippato = avatarEquippato == avatar.id

                    AvatarItem(
                        avatar = avatar,
                        isSbloccato = isSbloccato,
                        isEquippato = isEquippato,
                        onAvatarClick = {
                            if (isSbloccato) viewModel.selezionaAvatar(avatar.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarItem(
    avatar: Avatar? = null,
    avatarId: String? = null,
    isSbloccato: Boolean,
    isEquippato: Boolean,
    photoOverride: String? = null,
    labelOverride: String? = null,
    onAvatarClick: () -> Unit
) {
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

    val id = avatar?.id ?: avatarId ?: ""

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSbloccato, onClick = onAvatarClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .then(
                    if (isEquippato) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else Modifier
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isSbloccato) MaterialTheme.colorScheme.surfaceVariant else Color.LightGray)
        ) {
            if (photoOverride != null) {
                AsyncImage(
                    model = photoOverride,
                    contentDescription = id,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (!isSbloccato) ColorFilter.colorMatrix(grayscaleMatrix) else null
                )
            } else if (avatar != null) {
                Image(
                    painter = painterResource(avatar.drawableRes),
                    contentDescription = avatar.id,
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
        }

        Spacer(Modifier.height(8.dp))

        val textLabel = when {
            labelOverride != null -> if (isEquippato) "$labelOverride (Uso)" else labelOverride
            !isSbloccato -> "Liv. ${avatar?.livelloRichiesto}"
            isEquippato -> "In uso"
            else -> "Sbloccato"
        }

        Text(
            textLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isEquippato) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = if (isEquippato) FontWeight.ExtraBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
