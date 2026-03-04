package com.example.curiosillo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.curiosillo.ui.CuriosityScreen
import com.example.curiosillo.ui.HomeScreen
import com.example.curiosillo.ui.ProfileScreen
import com.example.curiosillo.ui.QuizScreen
import com.example.curiosillo.ui.theme.CuriosilloTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.curiosillo.ui.CategoryPickerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            CuriosilloTheme {
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home")      { HomeScreen(nav) }
                        composable("category_picker/curiosity") {
                            CategoryPickerScreen(nav, destinazione = "curiosity")
                        }
                        composable("category_picker/quiz") {
                            CategoryPickerScreen(nav, destinazione = "quiz")
                        }
                        composable("curiosity") { CuriosityScreen(nav) }
                        composable("quiz")      { QuizScreen(nav) }
                        composable("profile")   { ProfileScreen(nav) }
                    }
                }
            }
        }
    }
}
