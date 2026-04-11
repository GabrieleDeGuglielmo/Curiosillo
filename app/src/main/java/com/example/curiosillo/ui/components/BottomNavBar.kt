package com.example.curiosillo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

data class NavItem(
    val route: String,
    val icon:  ImageVector,
    val label: String
)

val navItems = listOf(
    NavItem("novita",  Icons.Default.AutoAwesome,   "Novità"),
    NavItem("ripasso", Icons.Default.Refresh,       "Ripasso"),
    NavItem("home",    Icons.Default.Home,          "Home"),
    NavItem("gioca",   Icons.Default.SportsEsports, "Gioca"),
    NavItem("profile", Icons.Default.Person,        "Profilo")
)

@Composable
fun CuriosilloBottomBar(nav: NavController) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val minBarHeight   = if (screenHeightDp < 640) 80.dp else 94.dp
    val fabOffset      = if (screenHeightDp < 640) (-34).dp else (-40).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier
                .heightIn(min = minBarHeight)
                .navigationBarsPadding()
        ) {
            navItems.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route

                if (index == 2) {
                    // Segnaposto centrale per il FAB
                    NavigationBarItem(
                        selected = isSelected,
                        onClick  = {
                            if (currentRoute != "home") {
                                nav.navigate("home") {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon  = { Spacer(Modifier.size(42.dp)) },
                        label = null,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                    )
                } else {
                    NavigationBarItem(
                        selected = isSelected,
                        onClick  = {
                            if (currentRoute == item.route) return@NavigationBarItem
                            nav.navigate(item.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = { Icon(item.icon, null, modifier = Modifier.size(26.dp)) },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        val isHomeSelected = currentRoute == "home"

        LargeFloatingActionButton(
            onClick = {
                if (currentRoute != "home") {
                    nav.navigate("home") {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
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
