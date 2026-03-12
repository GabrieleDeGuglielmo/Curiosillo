package com.example.curiosillo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class NavItem(
    val route: String,
    val icon:  ImageVector,
    val label: String
)

val navItems = listOf(
    NavItem("novita",  Icons.Default.AutoAwesome,       "Novità"),
    NavItem("ripasso", Icons.Default.Refresh,           "Ripasso"),
    NavItem("home",    Icons.Default.Home,              ""),
    NavItem("duello",  Icons.Default.SportsMartialArts, "Duello"),
    NavItem("profile", Icons.Default.Person,            "Profilo")
)

val navBarRoutes = setOf("home", "ripasso", "duello", "profile", "novita")

@Composable
fun CuriosilloBottomBar(nav: NavController) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Altezza adattiva: base 68dp + padding sistema (gesture bar / tasti)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val barHeight      = when {
        screenHeightDp < 640 -> 76.dp   // schermi piccoli
        else                 -> 89.dp   // schermi normali/grandi
    }
    val fabOffset      = when {
        screenHeightDp < 640 -> (-32).dp
        else                 -> (-36).dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. La barra di navigazione con padding per gesture/nav bar di sistema
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier
                .height(barHeight)
                .navigationBarsPadding()
        ) {
            navItems.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route

                if (index == 2) {
                    // Segnaposto centrale con ETICHETTA ripristinata
                    NavigationBarItem(
                        selected = isSelected,
                        onClick  = {
                            nav.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        // Spacer ridotto per lasciare spazio all'etichetta sotto il FAB
                        icon  = { Spacer(Modifier.size(40.dp)) },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                } else {
                    NavigationBarItem(
                        selected = isSelected,
                        onClick  = {
                            nav.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = { Icon(item.icon, null, modifier = Modifier.size(26.dp)) },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        // 2. Pulsante Home (FAB) rimpicciolito
        val isHomeSelected = currentRoute == "home"

        LargeFloatingActionButton(
            onClick = {
                nav.navigate("home") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
            },
            shape = CircleShape,
            containerColor = if (isHomeSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primaryContainer,
            contentColor   = if (isHomeSelected) Color.White
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .offset(y = fabOffset)
                .size(68.dp)
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}