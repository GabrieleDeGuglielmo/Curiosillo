package com.example.curiosillo

import android.content.pm.ActivityInfo
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
import com.example.curiosillo.ui.screens.admin.AdminBroadcastScreen
import com.example.curiosillo.ui.screens.admin.AdminCommentiScreen
import com.example.curiosillo.ui.screens.admin.AdminCuriositaScreen
import com.example.curiosillo.ui.screens.admin.AdminVotiScreen
import com.example.curiosillo.ui.screens.curiosity.CategoryPickerScreen
import com.example.curiosillo.ui.screens.curiosity.CuriosityScreen
import com.example.curiosillo.ui.screens.curiosity.RipassoScreen
import com.example.curiosillo.ui.screens.quiz.QuizScreen
import com.example.curiosillo.ui.screens.quiz.QuizStatsScreen
import com.example.curiosillo.ui.screens.user.BookmarkScreen
import com.example.curiosillo.ui.screens.user.EditProfileScreen
import com.example.curiosillo.ui.screens.user.HomeScreen
import com.example.curiosillo.ui.screens.user.LoginScreen
import com.example.curiosillo.ui.screens.user.PilloleNascosteScreen
import com.example.curiosillo.ui.screens.user.ProfileScreen
import com.example.curiosillo.ui.theme.CuriosilloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                    composable("profile")    {
                        ProfileScreen(
                            nav,
                            onLogout = {
                                nav.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            })
                    }
                    composable("edit_profile") { EditProfileScreen(nav) }
                    composable("admin_broadcast") { AdminBroadcastScreen(nav) }
                    composable("preferiti")  { BookmarkScreen(nav) }
                    composable("ripasso")    { RipassoScreen(nav) }
                    composable("quiz_stats") { QuizStatsScreen(nav) }
                    composable("pillole_nascoste") { PilloleNascosteScreen(nav) }
                    composable("duello") { DuelloScreen(nav) }
                    composable("admin_voti")      { AdminVotiScreen(nav) }
                    composable("admin_commenti")  { AdminCommentiScreen(nav) }
                    composable("admin_curiosita") { AdminCuriositaScreen(nav) }
                    composable("admin_curiosita/{externalId}") { back ->
                        val externalId = back.arguments?.getString("externalId")
                        AdminCuriositaScreen(nav, apriModificaId = externalId)
                    }
                    composable("novita") { NovitaScreen(nav) }
                    composable("category_picker/{dest}") { back ->
                        val dest = back.arguments?.getString("dest") ?: "curiosity"
                        CategoryPickerScreen(nav, dest)
                    }
                }
            }
        }
    }
}