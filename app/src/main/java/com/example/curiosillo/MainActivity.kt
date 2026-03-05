package com.example.curiosillo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.curiosillo.ui.*
import com.example.curiosillo.ui.theme.CuriosilloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as CuriosityApplication

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
