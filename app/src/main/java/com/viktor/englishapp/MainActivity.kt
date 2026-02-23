package com.viktor.englishapp // ПРОВЕРИ ПАКЕТА!

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
import com.viktor.englishapp.data.TokenManager // ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.ui.DashboardScreen // ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.ui.LoginScreen // ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.ui.ExpertScreen
import com.viktor.englishapp.ui.ExerciseListScreen
import com.viktor.englishapp.ui.SolveExerciseScreen
import com.viktor.englishapp.ui.theme.EnglishLearningAppTheme // ПРОВЕРИ ТЕМАТА!
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)
        val startDestination = if (tokenManager.getToken() != null) "dashboard" else "login"

        setContent {
            EnglishLearningAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDestination) {

                        // --- ЕКРАН 1: ВХОД ---
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- ЕКРАН 2: ГЛАВНО МЕНЮ ---
                        composable("dashboard") {
                            DashboardScreen(
                                onLogout = {
                                    tokenManager.clearToken()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onGoToExpert = {
                                    navController.navigate("expert_panel")
                                }, // <-- ТАЗИ ЗАПЕТАЯ БЕШЕ ИЗПУСНАТА!
                                onGoToExercises = {
                                    navController.navigate("exercise_list")
                                }
                            )
                        }

                        // --- ЕКРАН 3: ЕКСПЕРТЕН ПАНЕЛ ---
                        composable("expert_panel") {
                            ExpertScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // --- ЕКРАН 4: СПИСЪК С УПРАЖНЕНИЯ ---
                        composable("exercise_list") {
                            ExerciseListScreen(
                                onBack = { navController.popBackStack() },
                                onExerciseClick = { exercise ->
                                    // 1. Кодираме JSON текста, за да е безопасен за навигацията
                                    val encodedJson = java.net.URLEncoder.encode(exercise.content, "UTF-8")

                                    // 2. Предаваме кодирания текст
                                    navController.navigate("solve_exercise/$encodedJson")
                                }
                            )
                        }
                        composable(
                            route = "solve_exercise/{json}",
                            arguments = listOf(navArgument("json") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val encodedJson = backStackEntry.arguments?.getString("json") ?: ""

                            // Декодираме го обратно към оригинален JSON
                            val decodedJson = java.net.URLDecoder.decode(encodedJson, "UTF-8")

                            SolveExerciseScreen(
                                exerciseJson = decodedJson,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}