package com.example.curiosillo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.screens.*
import com.example.curiosillo.ui.theme.CuriosilloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val app = applicationContext as CuriosityApplication

        setContent {
            val isDarkMode by app.themePrefs.isDarkMode.collectAsState(initial = false)

            CuriosilloTheme(darkTheme = isDarkMode) {
                val nav = rememberNavController()

                // Punto di partenza: login se non loggato, home se già loggato
                val startDestination = if (FirebaseManager.isLoggato) "home" else "login"

                NavHost(nav, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(onLoginSuccesso = {
                            nav.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("home")       { HomeScreen(nav) }
                    composable("curiosity")  { CuriosityScreen(nav) }
                    composable("quiz")       { QuizScreen(nav) }
                    composable("profile")    { ProfileScreen(nav,
                        onLogout = {
                            nav.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        })
                    }
                    composable("preferiti")  { BookmarkScreen(nav) }
                    composable("ripasso")    { RipassoScreen(nav) }
                    composable("quiz_stats") { QuizStatsScreen(nav) }
                    composable("pillole_nascoste") { PilloleNascosteScreen(nav) }
                    composable("duello") { DuelloScreen(nav) }
                    composable("category_picker/{dest}") { back ->
                        val dest = back.arguments?.getString("dest") ?: "curiosity"
                        CategoryPickerScreen(nav, dest)
                    }
                }
            }
        }
    }
}
