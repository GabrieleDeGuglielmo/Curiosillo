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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.curiosillo.data.Avatar
import com.example.curiosillo.data.AvatarCatalogo
import com.example.curiosillo.domain.LivelloHelper
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
                items(
                    items = AvatarCatalogo.lista,
                    key = { it.id } // KEY OBBLIGATORIA per recycling efficiente
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
    avatar: Avatar,
    isSbloccato: Boolean,
    isEquippato: Boolean,
    onAvatarClick: () -> Unit
) {
    // PREVENZIONE ALLOCAZIONI: ColorMatrix in remember
    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

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
            Image(
                painter = painterResource(avatar.drawableRes),
                contentDescription = avatar.id,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                // Applichiamo il filtro scala di grigi se bloccato
                colorFilter = if (!isSbloccato) ColorFilter.colorMatrix(grayscaleMatrix) else null
            )

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
        
        if (!isSbloccato) {
            Text(
                "Liv. ${avatar.livelloRichiesto}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        } else if (isEquippato) {
            Text(
                "In uso",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            Text(
                "Sbloccato",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
