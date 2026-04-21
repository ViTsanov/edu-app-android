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
import com.viktor.englishapp.ui.AssignHomeworkScreen
import com.viktor.englishapp.ui.ClassroomDetailScreen
import com.viktor.englishapp.ui.ClassroomManagementScreen
import com.viktor.englishapp.ui.CreateTeacherExerciseScreen
import com.viktor.englishapp.ui.CreateTestScreen
import com.viktor.englishapp.ui.DashboardScreen
import com.viktor.englishapp.ui.EditExerciseScreen
import com.viktor.englishapp.ui.ExerciseListScreen
import com.viktor.englishapp.ui.ExerciseResultScreen
import com.viktor.englishapp.ui.ExpertActiveExercisesScreen
import com.viktor.englishapp.ui.ExpertScreen
import com.viktor.englishapp.ui.JoinClassroomScreen
import com.viktor.englishapp.ui.LearningPathScreen
import com.viktor.englishapp.ui.LoginScreen
import com.viktor.englishapp.ui.MyClassroomsScreen
import com.viktor.englishapp.ui.PendingExercisesScreen
import com.viktor.englishapp.ui.ProfileScreen
import com.viktor.englishapp.ui.RegisterScreen
import com.viktor.englishapp.ui.SolveExerciseScreen
import com.viktor.englishapp.ui.StudentHomeworkScreen
import com.viktor.englishapp.ui.StudentMonitoringScreen
import com.viktor.englishapp.ui.TeacherHomeworkListScreen
import com.viktor.englishapp.ui.TeacherHomeworkReviewScreen
import com.viktor.englishapp.ui.TeacherTestResultsScreen
import com.viktor.englishapp.ui.TestManagementScreen
import com.viktor.englishapp.ui.TestTakingScreen
import com.viktor.englishapp.ui.theme.EnglishLearningAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

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
                                    tokenManager.clearToken()
                                    sessionManager.clearSession()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onGoToProfile = { navController.navigate("profile") },
                                onGoToExpert = { navController.navigate("pending_exercises") },
                                onGoToExpertActive = { navController.navigate("expert_active_exercises") },
                                onGoToExercises = { navController.navigate("exercise_list") },
                                onGoToClassrooms = { navController.navigate("classroom_management") },
                                onCreateExercise = { navController.navigate("create_teacher_exercise") },
                                onCreateTest = { navController.navigate("create_test") },
                                onGoToPath = { navController.navigate("learning_path") },
                                onJoinClassroom = { navController.navigate("join_classroom") },
                                onGoToMyClassrooms = { navController.navigate("my_classrooms") },
                                onGoToHomework = { navController.navigate("student_homework") },
                                // FIX: was nested onGoToAssignHomework = { onGoToAssignHomework = {...} }
                                onGoToAssignHomework = { navController.navigate("teacher_homework_list") },
                                onGoToTestManagement = { navController.navigate("test_management") }
                            )
                        }

                        // ── SCREEN 4: PROFILE ────────────────────────────────
                        composable("profile") {
                            ProfileScreen(onBack = { navController.popBackStack() })
                        }

                        // ── SCREEN 5: EXPERT AI GENERATOR ───────────────────
                        composable("expert_panel") {
                            ExpertScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToPending = { navController.navigate("pending_exercises") },
                                onCreateManualExercise = { navController.navigate("create_teacher_exercise") }
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
                            val titleArg = backStackEntry.arguments?.getString("title") ?: ""
                            val contentArg = backStackEntry.arguments?.getString("content") ?: ""
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

                        // ── SCREEN 8: EXERCISE LIST ──────────────────────────
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
                                homeworkId = 0,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 10: EXPERT ACTIVE EXERCISES ──────────────
                        composable("expert_active_exercises") {
                            ExpertActiveExercisesScreen(onBack = { navController.popBackStack() })
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
                            CreateTeacherExerciseScreen(onBack = { navController.popBackStack() })
                        }

                        composable("create_test") {
                            CreateTestScreen(onBack = { navController.popBackStack() })
                        }

                        // ── SCREEN 12: STUDENT MONITORING (teacher) ──────────
                        composable(
                            route = "monitoring/{classroomId}",
                            arguments = listOf(navArgument("classroomId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val classroomId = backStackEntry.arguments?.getInt("classroomId") ?: 0
                            StudentMonitoringScreen(
                                classroomId = classroomId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── SCREEN 13: TEST TAKING (student) ────────────────
                        composable(
                            route = "test_taking/{testId}",
                            arguments = listOf(navArgument("testId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val testId = backStackEntry.arguments?.getInt("testId") ?: 0
                            TestTakingScreen(
                                testId = testId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── Join classroom ───────────────────────────────────
                        composable("join_classroom") {
                            JoinClassroomScreen(
                                onBack = { navController.popBackStack() },
                                onJoined = { navController.popBackStack() }
                            )
                        }

                        // ── Learning path ────────────────────────────────────
                        composable("learning_path") {
                            LearningPathScreen(
                                onBack = { navController.popBackStack() },
                                onStartExercise = { exercise ->
                                    val encodedJson = java.net.URLEncoder.encode(
                                        exercise.content_prompt, "UTF-8"
                                    )
                                    navController.navigate("solve_exercise/${exercise.id}/$encodedJson")
                                }
                            )
                        }

                        // ── Student: my classrooms ───────────────────────────
                        composable("my_classrooms") {
                            MyClassroomsScreen(
                                onBack = { navController.popBackStack() },
                                onGoToHomework = { navController.navigate("student_homework") },
                                onGoToJoin = { navController.navigate("join_classroom") },
                                onOpenClassroom = { classroomId ->
                                    navController.navigate("classroom_detail/$classroomId")
                                }
                            )
                        }

                        // ── Student: flat homework list ──────────────────────
                        composable("student_homework") {
                            StudentHomeworkScreen(
                                onBack = { navController.popBackStack() },
                                onStartExercise = { exerciseId, encodedJson ->
                                    navController.navigate("solve_exercise/$exerciseId/$encodedJson")
                                }
                            )
                        }

                        // ── Student: view exercise result ────────────────────
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

                        // ── Teacher: assign homework ─────────────────────────
                        composable("assign_homework") {
                            AssignHomeworkScreen(onBack = { navController.popBackStack() })
                        }

                        // ── Teacher: homework list + review ──────────────────
                        composable("teacher_homework_list") {
                            TeacherHomeworkListScreen(
                                onBack = { navController.popBackStack() },
                                onAssignNew = { navController.navigate("assign_homework") },
                                onViewSubmissions = { homeworkId, encodedTitle, encodedJson ->
                                    navController.navigate(
                                        "homework_review/$homeworkId/$encodedTitle/$encodedJson"
                                    )
                                }
                            )
                        }

                        // ── Teacher: manage tests ────────────────────────────
                        composable("test_management") {
                            TestManagementScreen(
                                onBack = { navController.popBackStack() },
                                onCreateTest = { navController.navigate("create_test") },
                                onViewResults = { testId ->
                                    navController.navigate("test_results/$testId")
                                }
                            )
                        }

                        // ── Teacher: test results per student ────────────────
                        composable(
                            route = "test_results/{testId}",
                            arguments = listOf(navArgument("testId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val testId = backStackEntry.arguments?.getInt("testId") ?: 0
                            TeacherTestResultsScreen(
                                testId = testId,
                                testTitle = "Резултати от теста",
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── Student: open classroom detail ───────────────────
                        composable(
                            route = "classroom_detail/{classroomId}",
                            arguments = listOf(navArgument("classroomId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val classroomId = backStackEntry.arguments?.getInt("classroomId") ?: 0
                            ClassroomDetailScreen(
                                classroomId = classroomId,
                                onBack = { navController.popBackStack() },
                                onOpenHomework = { hw ->
                                    if (!hw.submitted && hw.contentPrompt != null) {
                                        val encodedJson = java.net.URLEncoder.encode(
                                            hw.contentPrompt, "UTF-8"
                                        )
                                        val exerciseId = hw.exerciseId ?: hw.teacherExerciseId ?: 0
                                        navController.navigate(
                                            "homework_solve/${hw.id}/$exerciseId/$encodedJson"
                                        )
                                    } else {
                                        val exerciseId = hw.exerciseId ?: return@ClassroomDetailScreen
                                        navController.navigate("exercise_result/$exerciseId")
                                    }
                                },
                                onOpenTest = { testId ->
                                    navController.navigate("test_taking/$testId")
                                }
                            )
                        }

                        // ── Student: solve a homework exercise ───────────────
                        composable(
                            route = "homework_solve/{homeworkId}/{exerciseId}/{json}",
                            arguments = listOf(
                                navArgument("homeworkId") { type = NavType.IntType },
                                navArgument("exerciseId") { type = NavType.IntType },
                                navArgument("json") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val homeworkId = backStackEntry.arguments?.getInt("homeworkId") ?: 0
                            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                            val json = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("json") ?: "", "UTF-8"
                            )
                            // FIX: removed onFinished — SolveExerciseScreen only has onBack
                            SolveExerciseScreen(
                                exerciseId = exerciseId,
                                exerciseJson = json,
                                homeworkId = homeworkId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ── Teacher: see homework submissions ────────────────
                        composable(
                            route = "homework_review/{homeworkId}/{encodedTitle}/{encodedJson}",
                            arguments = listOf(
                                navArgument("homeworkId") { type = NavType.IntType },
                                navArgument("encodedTitle") { type = NavType.StringType },
                                navArgument("encodedJson") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val homeworkId = backStackEntry.arguments?.getInt("homeworkId") ?: 0
                            val title = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("encodedTitle") ?: "", "UTF-8"
                            )
                            val contentPrompt = java.net.URLDecoder.decode(
                                backStackEntry.arguments?.getString("encodedJson") ?: "", "UTF-8"
                            )
                            TeacherHomeworkReviewScreen(
                                homeworkId = homeworkId,
                                homeworkTitle = title,
                                contentPrompt = contentPrompt.ifEmpty { null },
                                onBack = { navController.popBackStack() }
                            )
                        }

                    } // end NavHost
                }
            }
        } // end setContent
    } // end onCreate

    // ── Session tracking ─────────────────────────────────────────
    override fun onStart() {
        super.onStart()
        val token = tokenManager.getToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.startSession("Bearer $token")
                val sessionId = (response["session_id"] as? Double)?.toInt() ?: return@launch
                sessionManager.saveSessionId(sessionId)
            } catch (_: Exception) {}
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
            } catch (_: Exception) {}
        }
    }
}

// ── SessionManager ─────────────────────────────────────────────
class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun saveSessionId(sessionId: Int) { prefs.edit { putInt("active_session_id", sessionId) } }
    fun getSessionId(): Int = prefs.getInt("active_session_id", -1)
    fun clearSession() { prefs.edit { remove("active_session_id") } }
}