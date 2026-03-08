package com.example.curiosillo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    NavItem("home",    Icons.Default.Home,              "Home"),
    NavItem("duello",  Icons.Default.SportsMartialArts, "Duello"),
    NavItem("profile", Icons.Default.Person,            "Profilo")
)

val navBarRoutes = setOf("home", "ripasso", "duello", "profile", "novita")

@Composable
fun CuriosilloBottomBar(nav: NavController) {
    val backStack    by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        navItems.forEachIndexed { index, item ->
            val isSelected = currentRoute == item.route
            val isHome     = index == 2

            if (isHome) {
                // ── FAB centrale che spunta fuori dalla barra ─────────────────
                NavigationBarItem(
                    selected = isSelected,
                    onClick  = {
                        nav.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Box(
                            modifier          = Modifier
                                .size(62.dp)
                                .offset(y = (-18).dp)
                                .clip(CircleShape),
                            contentAlignment  = Alignment.Center
                        ) {
                            // Ombra/sfondo
                            Surface(
                                modifier      = Modifier.size(62.dp),
                                shape         = CircleShape,
                                color         = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = if (isSelected) 10.dp else 4.dp,
                                tonalElevation  = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        item.icon, null,
                                        modifier = Modifier.size(30.dp),
                                        tint     = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            } else {
                val scale by animateFloatAsState(
                    targetValue   = if (isSelected) 1.15f else 1f,
                    animationSpec = tween(200),
                    label         = "scale_$index"
                )
                NavigationBarItem(
                    selected = isSelected,
                    onClick  = {
                        nav.navigate(item.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    icon = {
                        Icon(
                            item.icon, null,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}