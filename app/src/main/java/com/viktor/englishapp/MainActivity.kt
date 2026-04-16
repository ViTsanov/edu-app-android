package com.viktor.englishapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.ui.ClassroomManagementScreen
import com.viktor.englishapp.ui.DashboardScreen
import com.viktor.englishapp.ui.EditExerciseScreen
import com.viktor.englishapp.ui.ExerciseListScreen
import com.viktor.englishapp.ui.ExpertActiveExercisesScreen
import com.viktor.englishapp.ui.ExpertScreen
import com.viktor.englishapp.ui.LoginScreen
import com.viktor.englishapp.ui.PendingExercisesScreen
import com.viktor.englishapp.ui.ProfileScreen
import com.viktor.englishapp.ui.RegisterScreen
import com.viktor.englishapp.ui.SolveExerciseScreen
import com.viktor.englishapp.ui.StudentMonitoringScreen
import com.viktor.englishapp.ui.TestTakingScreen
import com.viktor.englishapp.ui.theme.EnglishLearningAppTheme
import kotlinx.coroutines.launch
import com.viktor.englishapp.ui.CreateTeacherExerciseScreen
import com.viktor.englishapp.ui.CreateTestScreen
import com.viktor.englishapp.ui.JoinClassroomScreen
import com.viktor.englishapp.ui.LearningPathScreen
import com.viktor.englishapp.ui.MyClassroomsScreen
import com.viktor.englishapp.ui.ExerciseResultScreen
import com.viktor.englishapp.ui.StudentHomeworkScreen
import com.viktor.englishapp.ui.AssignHomeworkScreen
import com.viktor.englishapp.ui.TestManagementScreen
import com.viktor.englishapp.ui.MyClassroomsScreen
import com.viktor.englishapp.ui.StudentHomeworkScreen
import com.viktor.englishapp.ui.AssignHomeworkScreen
import com.viktor.englishapp.ui.TestManagementScreen
import com.viktor.englishapp.ui.ExerciseResultScreen

class MainActivity : ComponentActivity() {

    // ── Managers initialised once in onCreate ────────────────────
    private lateinit var tokenManager: TokenManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        sessionManager = SessionManager(this)

        val startDestination = if (tokenManager.getToken() != null) "dashboard" else "login"

        setContent {
            EnglishLearningAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {

                        // ── SCREEN 1: LOGIN ──────────────────────────────────
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }

                        // ── SCREEN 2: REGISTER ───────────────────────────────
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // ── SCREEN 3: DASHBOARD ──────────────────────────────
                        composable("dashboard") {
                            DashboardScreen(
                                onLogout = {
                                    // FIX: use TokenManager instead of raw SharedPreferences
                                    tokenManager.clearToken()
                                    sessionManager.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
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
                                },
                                onGoToClassrooms = {
                                    navController.navigate("classroom_management")
                                },
                                onCreateExercise = {
                                    navController.navigate("create_teacher_exercise")
                                },   // NEW
                                onCreateTest = {
                                    navController.navigate("create_test")
                                },
                                onGoToPath = {
                                    navController.navigate("learning_path")
                                },       // ← ADD
                                onJoinClassroom = {
                                    navController.navigate("join_classroom")
                                },
                                onGoToMyClassrooms = {
                                    navController.navigate("my_classrooms")
                                },      // ← ADD
                                onGoToHomework = {
                                    navController.navigate("student_homework")
                                },        // ← ADD
                                onGoToAssignHomework = {
                                    navController.navigate("assign_homework")
                                },   // ← ADD
                                onGoToTestManagement = {
                                    navController.navigate("test_management")
                                }
                            )
                        }

                        // ── SCREEN 4: PROFILE ────────────────────────────────
                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 5: EXPERT AI GENERATOR ───────────────────
                        composable("expert_panel") {
                            ExpertScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToPending = {
                                    navController.navigate("pending_exercises")
                                },
                                onCreateManualExercise = {
                                    navController.navigate("create_teacher_exercise")
                                }
                            )
                        }

                        // ── SCREEN 6: PENDING EXERCISES (expert review) ──────
                        composable("pending_exercises") {
                            PendingExercisesScreen(
                                onBack = { navController.popBackStack() },
                                onEditExercise = { exerciseId, title, content ->
                                    val encodedContent = android.util.Base64.encodeToString(
                                        content.toByteArray(Charsets.UTF_8),
                                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                                    )
                                    val encodedTitle = android.util.Base64.encodeToString(
                                        title.toByteArray(Charsets.UTF_8),
                                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                                    )
                                    navController.navigate(
                                        "edit_exercise/$exerciseId/$encodedTitle/$encodedContent"
                                    )
                                }
                            )
                        }

                        // ── SCREEN 7: EDIT EXERCISE ──────────────────────────
                        // FIX: Only ONE definition here (Base64). Duplicate removed.
                        composable(
                            route = "edit_exercise/{exerciseId}/{title}/{content}",
                            arguments = listOf(
                                navArgument("exerciseId") { type = NavType.IntType },
                                navArgument("title") { type = NavType.StringType },
                                navArgument("content") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val exerciseId =
                                backStackEntry.arguments?.getInt("exerciseId") ?: return@composable
                            val titleArg =
                                backStackEntry.arguments?.getString("title") ?: ""
                            val contentArg =
                                backStackEntry.arguments?.getString("content") ?: ""

                            val title = String(
                                android.util.Base64.decode(
                                    titleArg,
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                                ),
                                Charsets.UTF_8
                            )
                            val content = String(
                                android.util.Base64.decode(
                                    contentArg,
                                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                                ),
                                Charsets.UTF_8
                            )

                            EditExerciseScreen(
                                exerciseId = exerciseId,
                                exerciseTitle = title,
                                initialContent = content,
                                onBack = { navController.popBackStack() },
                                onApproved = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 8: EXERCISE LIST (student learning path) ──
                        composable("exercise_list") {
                            ExerciseListScreen(
                                onBack = { navController.popBackStack() },
                                onExerciseClick = { exercise ->
                                    val encodedJson = java.net.URLEncoder.encode(
                                        exercise.content_prompt, "UTF-8"
                                    )
                                    navController.navigate("solve_exercise/${exercise.id}/$encodedJson")
                                }
                            )
                        }

                        // ── SCREEN 9: SOLVE EXERCISE ─────────────────────────
                        composable(
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
                                exerciseId = exerciseId,
                                exerciseJson = decodedJson,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 10: EXPERT ACTIVE EXERCISES ──────────────
                        composable("expert_active_exercises") {
                            ExpertActiveExercisesScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 11: CLASSROOM MANAGEMENT (teacher) ────────
                        composable("classroom_management") {
                            ClassroomManagementScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToMonitoring = { classroomId ->
                                    navController.navigate("monitoring/$classroomId")
                                }
                            )
                        }

                        composable("create_teacher_exercise") {
                            CreateTeacherExerciseScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("create_test") {
                            CreateTestScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 12: STUDENT MONITORING (teacher) ──────────
                        composable(
                            route = "monitoring/{classroomId}",
                            arguments = listOf(
                                navArgument("classroomId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val classroomId =
                                backStackEntry.arguments?.getInt("classroomId") ?: 0
                            StudentMonitoringScreen(
                                classroomId = classroomId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 13: TEST TAKING (student) ────────────────
                        composable(
                            route = "test_taking/{testId}",
                            arguments = listOf(
                                navArgument("testId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val testId = backStackEntry.arguments?.getInt("testId") ?: 0
                            TestTakingScreen(
                                testId = testId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // Join classroom
                        composable("join_classroom") {
                            JoinClassroomScreen(
                                onBack = { navController.popBackStack() },
                                onJoined = { navController.popBackStack() }
                            )
                        }

// Interactive learning path (replaces exercise_list for students)
                        composable("learning_path") {
                            LearningPathScreen(
                                onBack = { navController.popBackStack() },
                                onStartExercise = { exercise ->
                                    val encodedJson = java.net.URLEncoder.encode(exercise.content_prompt, "UTF-8")
                                    navController.navigate("solve_exercise/${exercise.id}/$encodedJson")
                                }
                            )
                        }


                        // Student: my classrooms
                        composable("my_classrooms") {
                            MyClassroomsScreen(
                                onBack = { navController.popBackStack() },
                                onGoToHomework = { navController.navigate("student_homework") },
                                onGoToJoin = { navController.navigate("join_classroom") }
                            )
                        }

// Student: homework
                        composable("student_homework") {
                            StudentHomeworkScreen(
                                onBack = { navController.popBackStack() },
                                onStartExercise = { exerciseId, encodedJson ->
                                    navController.navigate("solve_exercise/$exerciseId/$encodedJson")
                                }
                            )
                        }

// Student: view old exercise result
                        composable(
                            route = "exercise_result/{exerciseId}",
                            arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                            ExerciseResultScreen(
                                exerciseId = exerciseId,
                                onBack = { navController.popBackStack() }
                            )
                        }

// Teacher: assign homework
                        composable("assign_homework") {
                            AssignHomeworkScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

// Teacher: manage tests (activate/deactivate with opening time)
                        composable("test_management") {
                            TestManagementScreen(
                                onBack = { navController.popBackStack() },
                                onCreateTest = { navController.navigate("create_test") },
                                onViewResults = { testId ->
                                    // reuse existing test_results route if you have one
                                    // or just navigate to a simple screen
                                }
                            )
                        }

                    } // end NavHost
                }
            }
        } // end setContent
    } // end onCreate

    // ── Session tracking — FIX: moved here, OUT of setContent ────
    override fun onStart() {
        super.onStart()
        val token = tokenManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.startSession("Bearer $token")
                val sessionId = (response["session_id"] as? Double)?.toInt() ?: return@launch
                sessionManager.saveSessionId(sessionId)
            } catch (_: Exception) {
                // Non-critical — session tracking is best-effort
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val token = tokenManager.getToken() ?: return
        val sessionId = sessionManager.getSessionId()
        if (sessionId == -1) return
        lifecycleScope.launch {
            try {
                RetrofitClient.instance.endSession(sessionId, "Bearer $token")
                sessionManager.clearSession()
            } catch (_: Exception) {
                // Non-critical
            }
        }
    }
}

// ── SessionManager — placed at file level (valid Kotlin) ─────────
class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun saveSessionId(sessionId: Int) {
        prefs.edit { putInt("active_session_id", sessionId) }
    }

    fun getSessionId(): Int = prefs.getInt("active_session_id", -1)

    fun clearSession() {
        prefs.edit { remove("active_session_id") }
    }
}