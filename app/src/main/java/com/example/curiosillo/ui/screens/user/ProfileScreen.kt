package com.example.curiosillo.ui.screens.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.Avatar
import com.example.curiosillo.data.AvatarCatalogo
import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.domain.LivelloHelper
import com.example.curiosillo.ui.components.CuriosilloBottomBar
import com.example.curiosillo.ui.components.GamificationBanner
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavController, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val scope = rememberCoroutineScope()

    val vm: ProfileViewModel =
        viewModel(
            factory =
                ProfileViewModel.Factory(
                    app.repository,
                    app.gamificationPrefs,
                    app.contentPrefs,
                    ctx
                )
        )
    val state by vm.state.collectAsState()

    // Ricarica statistiche ogni volta che questa schermata e' in cima allo stack
    val navBackStack by nav.currentBackStackEntryAsState()
    LaunchedEffect(navBackStack) {
        if (navBackStack?.destination?.route == "profile") vm.caricaStatistiche()
    }

    var showAdminSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEliminaDialog by remember { mutableStateOf(false) }
    var badgeSbloccatoAppena by remember { mutableStateOf<BadgeSbloccato?>(null) }

    val mainSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMainSheet by remember { mutableStateOf(false) }

    // Popup per la scelta dell'Avatar
    var showAvatarSheet by remember { mutableStateOf(false) }
    val avatarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Sub-sheet Selezione Avatar ─────────────────────────────────────────────
    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = avatarSheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(
                    "Scegli il tuo Avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Sblocca nuovi personaggi salendo di livello!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                val livelloAttuale =
                    remember(state.xpTotali) { LivelloHelper.daXp(state.xpTotali).numero }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
                ) {
                    // 1. Opzione Google se disponibile
                    if (state.isGoogleUser && state.photoUrl != null) {
                        item(key = "google_option") {
                            AvatarGridItem(
                                avatar = Avatar("google", 1, 0), // Dummy per l'ID
                                isSbloccato = true,
                                isEquippato = state.avatarEquippato == "google",
                                photoOverride = state.photoUrl,
                                labelOverride = "Google",
                                onSelect = {
                                    scope.launch {
                                        vm.selezionaAvatar("google")
                                    }
                                }
                            )
                        }
                    }

                    // 2. Catalogo standard (include l'uovo)
                    items(AvatarCatalogo.lista, key = { it.id }) { avatar ->
                        val isSbloccato = livelloAttuale >= avatar.livelloRichiesto
                        val isEquippato = state.avatarEquippato == avatar.id

                        AvatarGridItem(
                            avatar = avatar,
                            isSbloccato = isSbloccato,
                            isEquippato = isEquippato,
                            onSelect = {
                                scope.launch {
                                    vm.selezionaAvatar(avatar.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ── BottomSheet azioni profilo (Impostazioni, Preferiti, etc.) ────────────────
    if (showMainSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMainSheet = false },
            sheetState = mainSheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                AzioneItem(
                    icon = Icons.Default.Settings,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "Impostazioni",
                    sub = "Tema, musica e altre preferenze"
                ) {
                    scope.launch { mainSheetState.hide() }.invokeOnCompletion {
                        showMainSheet = false
                        nav.navigate("settings")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                AzioneItem(
                    icon = Icons.Default.Bookmark,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "I miei preferiti (${state.totaleBookmark})",
                    sub = "Curiosità salvate con il segnalibro"
                ) {
                    scope.launch { mainSheetState.hide() }.invokeOnCompletion {
                        showMainSheet = false
                        nav.navigate("preferiti")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                AzioneItem(
                    icon = Icons.Default.VisibilityOff,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    label = "Pillole nascoste (${state.totaleIgnorate})",
                    sub = "Curiosità contrassegnate come \"non mi interessa\""
                ) {
                    scope.launch { mainSheetState.hide() }.invokeOnCompletion {
                        showMainSheet = false
                        nav.navigate("pillole_nascoste")
                    }
                }

                if (state.isAdmin) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    AzioneItemNav(
                        icon = Icons.Default.Shield,
                        tint = Color(0xFF7B2D8B),
                        label = "Admin",
                        sub = "Gestione contenuti e utenti"
                    ) { showAdminSheet = true }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                AzioneItemNav(
                    icon = Icons.Default.ManageAccounts,
                    tint = Error,
                    label = "Account",
                    sub = "Opzioni account"
                ) { showAccountSheet = true }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Sub-sheet Admin ──────────────────────────────────────────────────────
    if (showAdminSheet) {
        val adminSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAdminSheet = false },
            sheetState = adminSheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(
                    "🛡 Admin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                AzioneItem(
                    icon = Icons.Default.BarChart,
                    tint = Color(0xFF7B2D8B),
                    label = "Segnalazioni",
                    sub = "Visualizza segnalazioni alle curiosità"
                ) {
                    scope.launch { adminSheetState.hide() }.invokeOnCompletion {
                        showAdminSheet = false
                        nav.navigate("admin_voti")
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AzioneItem(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    tint = Color(0xFF7B2D8B),
                    label = "Moderazione commenti",
                    sub = "Controlla ed elimina i commenti degli utenti"
                ) {
                    scope.launch { adminSheetState.hide() }.invokeOnCompletion {
                        showAdminSheet = false
                        nav.navigate("admin_commenti")
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AzioneItem(
                    icon = Icons.Default.People,
                    tint = Color(0xFF7B2D8B),
                    label = "Gestione utenti",
                    sub = "Visualizza, cerca o banna gli utenti"
                ) {
                    scope.launch { adminSheetState.hide() }.invokeOnCompletion {
                        showAdminSheet = false
                        nav.navigate("admin_utenti")
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AzioneItem(
                    icon = Icons.Default.Edit,
                    tint = Color(0xFF7B2D8B),
                    label = "Gestione curiosità",
                    sub = "Aggiungi, modifica o importa curiosità"
                ) {
                    scope.launch { adminSheetState.hide() }.invokeOnCompletion {
                        showAdminSheet = false
                        nav.navigate("admin_curiosita")
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AzioneItem(
                    icon = Icons.Default.Campaign,
                    tint = Color(0xFF7B2D8B),
                    label = "Comunicazioni",
                    sub = "Invia un messaggio broadcast a tutti gli utenti"
                ) {
                    scope.launch { adminSheetState.hide() }.invokeOnCompletion {
                        showAdminSheet = false
                        nav.navigate("admin_broadcast")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Sub-sheet Account ─────────────────────────────────────────────────────
    if (showAccountSheet) {
        val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            sheetState = accountSheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(
                    "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                AzioneItem(
                    icon = Icons.Default.Edit,
                    tint = MaterialTheme.colorScheme.primary,
                    label = "Modifica profilo",
                    sub =
                        if (state.isGoogleUser) "Cambia il tuo username"
                        else "Cambia il tuo username o password"
                ) {
                    scope.launch { accountSheetState.hide() }.invokeOnCompletion {
                        showAccountSheet = false
                        nav.navigate("edit_profile")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                AzioneItem(
                    icon = Icons.Default.RestartAlt,
                    tint = Error,
                    label = "Resetta progressi",
                    sub = "Azzera XP, streak, badge e pillole lette"
                ) {
                    scope.launch { accountSheetState.hide() }.invokeOnCompletion {
                        showAccountSheet = false
                        showResetDialog = true
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AzioneItem(
                    icon = Icons.Default.Logout,
                    tint = Error,
                    label = "Esci dall'account",
                    sub = "Verrai disconnesso da questo dispositivo"
                ) {
                    scope.launch { accountSheetState.hide() }.invokeOnCompletion {
                        showAccountSheet = false
                        showLogoutDialog = true
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AzioneItem(
                    icon = Icons.Default.DeleteForever,
                    tint = Error,
                    label = "Elimina account",
                    sub = "Cancella account e tutti i dati in modo permanente"
                ) {
                    scope.launch { accountSheetState.hide() }.invokeOnCompletion {
                        showAccountSheet = false
                        showEliminaDialog = true
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Dialog badge appena sbloccato ─────────────────────────────────────────
    badgeSbloccatoAppena?.let { badge ->
        AlertDialog(
            onDismissRequest = { badgeSbloccatoAppena = null },
            icon = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Text(
                    "Badge sbloccato!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        badge.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.6.dp))
                    Text(
                        badge.descrizione,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { badgeSbloccatoAppena = null }) { Text("Ottimo!") }
            }
        )
    }

    // ── Dialog reset progressi ────────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = Error) },
            title = {
                Text(
                    "Resetta i progressi?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Tutte le curiosità torneranno a \"non lette\", il quiz verrà bloccato " +
                            "e XP, streak e badge verranno azzerati. Questa azione non può essere annullata.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.resetProgressi()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Sì, resetta") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Annulla") }
            }
        )
    }

    // ── Dialog logout ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, null, tint = Error) },
            title = {
                Text(
                    "Esci dall'account?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "I tuoi progressi sono salvati su cloud e saranno disponibili al prossimo accesso.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout(onLogout)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Esci") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("Annulla") }
            }
        )
    }

    // ── Dialog elimina account ────────────────────────────────────────────────
    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isEliminazioneInCorso) showEliminaDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Error) },
            title = {
                Text(
                    "Elimina account?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Verranno eliminati permanentemente il tuo account e tutti i dati associati: " +
                            "XP, streak, badge, pillole lette e preferiti.\n\nQuesta azione è irreversibile.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.eliminaAccount(onLogout) },
                    enabled = !state.isEliminazioneInCorso,
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    if (state.isEliminazioneInCorso) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sì, elimina tutto")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEliminaDialog = false },
                    enabled = !state.isEliminazioneInCorso
                ) { Text("Annulla") }
            }
        )
    }

    val gradientBg =
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.background
            )
        )
    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Il mio profilo") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (nav.currentBackStackEntry
                                        ?.lifecycle
                                        ?.currentState ==
                                    Lifecycle.State.RESUMED
                                ) {
                                    nav.popBackStack()
                                }
                            }
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                    },
                    actions = {
                        IconButton(onClick = { showMainSheet = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                "Azioni profilo",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                )
            },
            bottomBar = { CuriosilloBottomBar(nav) },
            containerColor = Color.Transparent
        ) { pad ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    Modifier.fillMaxSize()
                        .padding(pad)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Sezione account ───────────────────────────────────────────
                    if (state.isLoggato) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.primaryContainer
                                ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Foto profilo circolare con stile richiesto
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    // Deriviamo un teal più chiaro manualmente
                                    val lightTeal = Color(0xFF299183)

                                    Box(
                                        modifier = Modifier.size(92.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Ombra e Cerchio esterno Teal (Primary)
                                        Box(
                                            modifier =
                                                Modifier.size(80.dp)
                                                    .shadow(8.dp, CircleShape)
                                                    .border(
                                                        5.dp,
                                                        primaryColor,
                                                        CircleShape
                                                    )
                                                    .padding(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                                    .clickable {
                                                        showAvatarSheet = true
                                                    },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val currentAvatar = state.avatarEquippato

                                            if (currentAvatar == "google" && state.photoUrl != null) {
                                                AsyncImage(
                                                    model = state.photoUrl,
                                                    contentDescription = "Foto profilo",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                val avatar =
                                                    remember(currentAvatar) {
                                                        AvatarCatalogo.trovaPerId(currentAvatar)
                                                    }
                                                Image(
                                                    painter =
                                                        painterResource(avatar.drawableRes),
                                                    contentDescription = "Avatar",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        // Cerchietto matita/Face per cambio avatar
                                        Box(
                                            modifier =
                                                Modifier.size(30.dp)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 2.dp, y = (-2).dp)
                                                    .shadow(6.dp, CircleShape)
                                                    .background(lightTeal, CircleShape)
                                                    .border(
                                                        2.5.dp,
                                                        primaryColor,
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        showAvatarSheet = true
                                                    },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Face,
                                                contentDescription = "Cambia Avatar",
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            state.username.ifBlank { "Utente" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (state.email.isNotBlank()) {
                                            Text(
                                                state.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color =
                                                    MaterialTheme.colorScheme
                                                        .onPrimaryContainer.copy(
                                                            alpha = 0.7f
                                                        ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Banner XP
                    GamificationBanner(
                        xpTotali = state.xpTotali,
                        streakCorrente = state.streakCorrente
                    )
                    Spacer(Modifier.height(8.dp))

                    // Streak massima + info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Streak massima: ${state.streakMassima} ${if (state.streakMassima == 1) "giorno" else "giorni"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                        Spacer(Modifier.width(6.6.dp))
                        var showStreakInfo by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showStreakInfo = true },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                "Info streak",
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                        if (showStreakInfo) {
                            AlertDialog(
                                onDismissRequest = { showStreakInfo = false },
                                icon = { Text("🔥", fontSize = 32.sp) },
                                title = {
                                    Text(
                                        "Come funziona la streak?",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                text = {
                                    Text(
                                        "La streak conta i giorni consecutivi in cui hai letto almeno " +
                                                "una pillola e premuto \"Ho imparato!\"\n\nSe salti un giorno, " +
                                                "la streak riparte da 1.\n\nPiù alta è la streak, più XP bonus " +
                                                "guadagni ogni giorno!",
                                        textAlign = TextAlign.Center,
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.6f
                                            )
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showStreakInfo = false }) {
                                        Text("Capito!")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // ── Statistiche ───────────────────────────────────────────────
                    Text(
                        "Le mie statistiche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            Modifier.weight(1f),
                            "${state.curiositàImparate}",
                            "Curiosità\nimparate",
                            MaterialTheme.colorScheme.primary
                        )
                        StatCard(
                            Modifier.weight(1f),
                            "${state.totalCuriosità}",
                            "Curiosità\ntotali",
                            MaterialTheme.colorScheme.secondary
                        )
                        StatCard(
                            Modifier.weight(1f),
                            "${state.quizDisponibili}",
                            "Quiz non\nrisposti",
                            MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (state.totalCuriosità > 0) {
                        Spacer(Modifier.height(20.dp))
                        val pct = state.curiositàImparate.toFloat() / state.totalCuriosità
                        Text(
                            "Progresso complessivo — ${(pct * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // ── Scoperte ───────────────────────────────────────────
                    Spacer(Modifier.height(28.dp))
                    AzioneItemNav(
                        icon = Icons.Default.AutoAwesome,
                        tint = MaterialTheme.colorScheme.primary,
                        label = "Scoperte",
                        sub = "Le tue scoperte"
                    ) { nav.navigate("scoperte") }

                    // ── Badge personali ───────────────────────────
                    Spacer(Modifier.height(8.dp))
                    AzioneItemNav(
                        icon = Icons.Default.EmojiEvents,
                        tint = MaterialTheme.colorScheme.primary,
                        label = "Badge personali",
                        sub =
                            "Hai sbloccato ${state.badgeSbloccati.size} badge su ${BadgeCatalogo.tutti.size}"
                    ) { nav.navigate("badges") }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    } // Box gradient
}

@Composable
private fun AvatarGridItem(
    avatar: Avatar,
    isSbloccato: Boolean,
    isEquippato: Boolean,
    photoOverride: String? = null,
    labelOverride: String? = null,
    onSelect: () -> Unit
) {
    val grayscaleMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isSbloccato, onClick = onSelect)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier =
                Modifier.size(75.dp)
                    .then(
                        if (isEquippato)
                            Modifier.border(
                                3.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                        else Modifier
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSbloccato) MaterialTheme.colorScheme.surfaceVariant
                        else Color.LightGray
                    ),
            contentAlignment = Alignment.Center
        ) {
            if (photoOverride != null) {
                AsyncImage(
                    model = photoOverride,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter =
                        if (!isSbloccato) ColorFilter.colorMatrix(grayscaleMatrix) else null
                )
            } else {
                Image(
                    painter = painterResource(avatar.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter =
                        if (!isSbloccato) ColorFilter.colorMatrix(grayscaleMatrix) else null
                )
            }

            if (!isSbloccato) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        val textLabel =
            when {
                labelOverride != null ->
                    if (isEquippato) "$labelOverride (In uso)" else labelOverride
                !isSbloccato -> "Liv. ${avatar.livelloRichiesto}"
                isEquippato -> "In uso"
                else -> ""
            }

        Text(
            text = textLabel,
            style = MaterialTheme.typography.labelSmall,
            color =
                if (isEquippato) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AzioneItemNav(
    icon: ImageVector,
    tint: Color,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = tint.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AzioneItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, valore: String, etichetta: String, colore: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colore),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            Modifier.padding(vertical = 20.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valore, fontSize = 32.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                etichetta,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}