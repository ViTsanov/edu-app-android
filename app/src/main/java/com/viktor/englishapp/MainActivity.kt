package com.viktor.englishapp // ПРОВЕРИ ПАКЕТА!
import com.viktor.englishapp.ui.EditExerciseScreen
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
import com.viktor.englishapp.ui.PendingExercisesScreen
import com.viktor.englishapp.ui.ProfileScreen


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
                        // ... съществуващият ти код за login ...
                        composable("login") {
                            com.viktor.englishapp.ui.LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                // 🟢 ДОБАВЯМЕ КОМАНДАТА КЪМ LOGIN ЕКРАНА ДА ОТВАРЯ РЕГИСТРАЦИЯТА
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }

                        // 🟢 НОВИЯТ ЕКРАН ЗА РЕГИСТРАЦИЯ
                        composable("register") {
                            com.viktor.englishapp.ui.RegisterScreen(
                                onRegisterSuccess = {
                                    // При успешна регистрация, връщаме потребителя към логин екрана
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack() // Връщаме се назад към login
                                }
                            )
                        }

                        // --- ЕКРАН 2: ГЛАВНО МЕНЮ ---
                        // Главното табло след успешен вход
                        composable("dashboard") {
                            com.viktor.englishapp.ui.DashboardScreen(
                                onLogout = {
                                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    prefs.edit().remove("jwt_token").apply()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                // 🟢 ЕТО ГО РЕШЕНИЕТО НА ГРЕШКАТА:
                                onGoToProfile = {
                                    navController.navigate("profile")
                                },
                                onGoToExpert = {
                                    navController.navigate("pending_exercises")
                                },
                                onGoToExpertActive = {
                                    navController.navigate("expert_active_exercises")
                                },
                                onGoToExercises = {
                                    navController.navigate("exercise_list")
                                }
                            )
                        }

                        // 🟢 ЕТО ГО НОВИЯТ МАРШРУТ ЗА ПРОФИЛА:
                        composable("profile") {
                            com.viktor.englishapp.ui.ProfileScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // --- ЕКРАН 3: ЕКСПЕРТЕН ПАНЕЛ ---
                        composable("expert_panel") {
                            ExpertScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToPending = { navController.navigate("pending_exercises")}
                            )
                        }

                        // 🟢 НОВО: Маршрутът за екрана за одобряване
                        // 1. Маршрутът за екрана за одобряване
                        composable("pending_exercises") {
                            PendingExercisesScreen(
                                onBack = { navController.popBackStack() },
                                // 🟢 ТУК ПОДАВАМЕ ЛИПСВАЩИЯ ПАРАМЕТЪР:
                                onEditExercise = { exerciseId, title, content ->
                                    val encodedContent = android.util.Base64.encodeToString(content.toByteArray(Charsets.UTF_8), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                                    val encodedTitle = android.util.Base64.encodeToString(title.toByteArray(Charsets.UTF_8), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                                    navController.navigate("edit_exercise/$exerciseId/$encodedTitle/$encodedContent")
                                }
                            )
                        }

                        // 2. Новият екран за редактиране, към който отиваме
                        composable(
                            route = "edit_exercise/{exerciseId}/{title}/{content}",
                            arguments = listOf(
                                navArgument("exerciseId") { type = NavType.IntType },
                                navArgument("title") { type = NavType.StringType },
                                navArgument("content") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: return@composable
                            val titleArg = backStackEntry.arguments?.getString("title") ?: ""
                            val contentArg = backStackEntry.arguments?.getString("content") ?: ""

                            val title = String(android.util.Base64.decode(titleArg, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP), Charsets.UTF_8)
                            val content = String(android.util.Base64.decode(contentArg, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP), Charsets.UTF_8)

                            com.viktor.englishapp.ui.EditExerciseScreen(
                                exerciseId = exerciseId,
                                exerciseTitle = title,
                                initialContent = content,
                                onBack = { navController.popBackStack() },
                                onApproved = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "edit_exercise/{exerciseId}/{title}/{content}",
                            arguments = listOf(
                                navArgument("exerciseId") { type = NavType.IntType },
                                navArgument("title") { type = NavType.StringType },
                                navArgument("content") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: return@composable
                            val title = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
                            val content = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("content") ?: "", "UTF-8")

                            EditExerciseScreen(
                                exerciseId = exerciseId,
                                exerciseTitle = title,
                                initialContent = content,
                                onBack = { navController.popBackStack() },
                                onApproved = {
                                    // Връщаме се в предишния екран, когато е одобрено
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- ЕКРАН 4: СПИСЪК С УПРАЖНЕНИЯ ---
                        composable("exercise_list") {
                            ExerciseListScreen(
                                onBack = { navController.popBackStack() },
                                onExerciseClick = { exercise ->
                                    // 1. Кодираме JSON текста (вече се казва content_prompt)
                                    val encodedJson = java.net.URLEncoder.encode(exercise.content_prompt, "UTF-8")
                                    val exerciseId = exercise.id

                                    // 2. Предаваме ID-то и кодирания текст
                                    navController.navigate("solve_exercise/$exerciseId/$encodedJson")
                                }
                            )
                        }
                        composable(
                            // Вече очакваме и ID, и JSON
                            route = "solve_exercise/{id}/{json}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.IntType },
                                navArgument("json") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getInt("id") ?: 0
                            val encodedJson = backStackEntry.arguments?.getString("json") ?: ""

                            val decodedJson = java.net.URLDecoder.decode(encodedJson, "UTF-8")

                            SolveExerciseScreen(
                                exerciseId = exerciseId, // Подаваме ID-то на екрана!
                                exerciseJson = decodedJson,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Екран за разглеждане на активни упражнения от експерт
                        composable("expert_active_exercises") {
                            com.viktor.englishapp.ui.ExpertActiveExercisesScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}