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
import com.example.curiosillo.ui.screens.BookmarkScreen
import com.example.curiosillo.ui.screens.CategoryPickerScreen
import com.example.curiosillo.ui.screens.CuriosityScreen
import com.example.curiosillo.ui.screens.HomeScreen
import com.example.curiosillo.ui.screens.ProfileScreen
import com.example.curiosillo.ui.screens.QuizScreen
import com.example.curiosillo.ui.screens.QuizStatsScreen
import com.example.curiosillo.ui.screens.RipassoScreen
import com.example.curiosillo.ui.theme.CuriosilloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val app = applicationContext as CuriosityApplication
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val isDarkMode by app.themePrefs.isDarkMode.collectAsState(initial = false)

            CuriosilloTheme (darkTheme = isDarkMode) {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home")                     { HomeScreen(nav) }
                    composable("curiosity")                { CuriosityScreen(nav) }
                    composable("quiz")                     { QuizScreen(nav) }
                    composable("profile")                  { ProfileScreen(nav) }
                    composable("preferiti")                { BookmarkScreen(nav) }
                    composable("ripasso")                  { RipassoScreen(nav) }
                    composable("quiz_stats")               { QuizStatsScreen(nav) }
                    composable("category_picker/{dest}") { back ->
                        val dest = back.arguments?.getString("dest") ?: "curiosity"
                        CategoryPickerScreen(nav, dest)
                    }
                }
            }
        }
    }
}
