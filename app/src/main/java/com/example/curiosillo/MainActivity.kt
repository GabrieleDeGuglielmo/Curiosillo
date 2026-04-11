package com.example.curiosillo

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.screens.*
import com.example.curiosillo.ui.screens.admin.*
import com.example.curiosillo.ui.screens.curiosity.CategoryPickerScreen
import com.example.curiosillo.ui.screens.curiosity.CuriosityScreen
import com.example.curiosillo.ui.screens.curiosity.RipassoScreen
import com.example.curiosillo.ui.screens.quiz.QuizScreen
import com.example.curiosillo.ui.screens.quiz.QuizStatsScreen
import com.example.curiosillo.ui.screens.user.*
import com.example.curiosillo.ui.theme.CuriosilloTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var musicManager: MusicManager
    private var isMusicEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        musicManager = MusicManager(this)
        
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val app = applicationContext as CuriosityApplication

        lifecycleScope.launch {
            app.themePrefs.isMusicEnabled.collectLatest { enabled ->
                isMusicEnabled = enabled
                if (enabled) musicManager.start() else musicManager.stop()
            }
        }

        setContent {
            val isDarkMode by app.themePrefs.isDarkMode.collectAsState(initial = false)

            CuriosilloTheme(darkTheme = isDarkMode) {
                val nav = rememberNavController()

                // Punto di partenza: login se non loggato, home se già loggato
                val startDestination = if (FirebaseManager.isLoggato) "home" else "login"

                NavHost(nav, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            nav = nav,
                            onLoginSuccesso = {
                                nav.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = "home",
                        enterTransition = { fadeIn(tween(300)) },
                        exitTransition = { fadeOut(tween(300)) },
                        popEnterTransition = { fadeIn(tween(300)) },
                        popExitTransition = { fadeOut(tween(300)) }
                    ) { HomeScreen(nav) }
                    
                    composable("curiosity")  { CuriosityScreen(nav) }
                    composable("quiz")       { QuizScreen(nav) }
                    
                    composable(
                        route = "profile",
                        exitTransition = {
                            if (targetState.destination.route == "badges" || targetState.destination.route == "scoperte") {
                                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(100))
                            } else {
                                fadeOut(tween(300))
                            }
                        },
                        popExitTransition = { fadeOut(tween(300)) }
                    ) {
                        ProfileScreen(
                            nav,
                            onLogout = {
                                nav.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            })
                    }

                    composable(
                        route = "badges",
                        enterTransition = { 
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)) 
                        },
                        exitTransition = { 
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(300)) 
                        },
                        popEnterTransition = { 
                            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300)) 
                        },
                        popExitTransition = { 
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)) 
                        }
                    ) { BadgeScreen(nav) }

                    composable(
                        route = "scoperte",
                        enterTransition = { 
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300)) 
                        },
                        exitTransition = { 
                            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(300)) 
                        },
                        popEnterTransition = { 
                            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300)) 
                        },
                        popExitTransition = { 
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)) 
                        }
                    ) { ScoperteScreen(nav) }

                    composable("edit_profile") { EditProfileScreen(nav) }
                    composable("supporto") { SupportoScreen(nav) }
                    composable("supporto_bug") { BugReportScreen(nav) }
                    composable("supporto_curiosita") { SuggerimentoCuriositaScreen(nav) }
                    composable("supporto_suggerimento") { SuggerimentoScreen(nav) }
                    composable("supporto_detail/{title}") { back ->
                        val title = back.arguments?.getString("title") ?: "Supporto"
                        SupportoDetailScreen(nav, title)
                    }
                    composable("admin_broadcast") { AdminBroadcastScreen(nav) }
                    composable("preferiti")  { BookmarkScreen(nav) }
                    composable("ripasso")    { RipassoScreen(nav) }
                    composable("quiz_stats") { QuizStatsScreen(nav) }
                    composable("pillole_nascoste") { PilloleNascosteScreen(nav) }
                    composable("duello") { DuelloScreen(nav) }
                    composable("admin_voti")      { AdminVotiScreen(nav) }
                    composable("admin_commenti")  { AdminCommentiScreen(nav) }
                    composable("admin_utenti")    { AdminUtentiScreen(nav) }
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
                    
                    composable(
                        route = "ar_screen",
                        enterTransition = { fadeIn(tween(300)) },
                        exitTransition = { fadeOut(tween(300)) },
                        popEnterTransition = { fadeIn(tween(300)) },
                        popExitTransition = { fadeOut(tween(300)) }
                    ) { ArScreen(nav) }

                    composable("settings") { SettingsScreen(nav) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isMusicEnabled) musicManager.start()
    }

    override fun onStop() {
        super.onStop()
        musicManager.stop()
    }
}
