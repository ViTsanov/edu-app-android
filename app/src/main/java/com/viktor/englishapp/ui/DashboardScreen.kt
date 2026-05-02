package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.UserProfile
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    var userProfile by mutableStateOf<UserProfile?>(null)
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun loadProfile(tokenManager: TokenManager) {
        val token = tokenManager.getToken()
        if (token == null) { errorMessage = "Липсва токен."; isLoading = false; return }
        viewModelScope.launch {
            try {
                userProfile = RetrofitClient.instance.getMyProfile("Bearer $token")
            } catch (e: Exception) {
                errorMessage = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToExpert: () -> Unit,            // → pending_exercises
    onGoToExpertActive: () -> Unit,      // → expert_active_exercises
    onGoToClassrooms: () -> Unit,
    onCreateExercise: () -> Unit,
    onCreateTest: () -> Unit,
    onGoToPath: () -> Unit,
    onJoinClassroom: () -> Unit,
    onGoToMyClassrooms: () -> Unit,
    onGoToHomework: () -> Unit,
    onGoToAssignHomework: () -> Unit,
    onGoToTestManagement: () -> Unit,
    onGoToExpertPanel: () -> Unit = {},  // NEW → expert_panel (AI generator)
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.loadProfile(tokenManager) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EnglishApp") },
                actions = {
                    IconButton(onClick = onGoToProfile) {
                        Icon(
                            Icons.Default.AccountCircle, "Профил",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Зареждане на профила...")
                }
                viewModel.errorMessage.isNotEmpty() -> {
                    Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onLogout) { Text("Обратно към Вход") }
                }
                else -> {
                    val profile = viewModel.userProfile

                    Text(
                        "Здравей, ${profile?.username}! 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        profile?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(32.dp))

                    when (profile?.role_id) {

                        // ── TEACHER ───────────────────────────────
                        2 -> {
                            Text(
                                "Учителско табло",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            DashBtn(onClick = onGoToClassrooms, icon = Icons.Default.School, label = "МОИ КЛАСОВЕ")
                            Spacer(Modifier.height(12.dp))
                            DashBtn(onClick = onCreateExercise, icon = Icons.Default.Edit, label = "СЪЗДАЙ НОВО УПРАЖНЕНИЕ",
                                containerColor = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(12.dp))
                            DashBtn(onClick = onCreateTest, icon = Icons.Default.Quiz, label = "СЪЗДАЙ НОВ ТЕСТ",
                                containerColor = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(12.dp))
                            OutlinedDashBtn(onClick = onGoToExpertActive, icon = Icons.AutoMirrored.Filled.List, label = "АКТИВНИ УПРАЖНЕНИЯ")
                            Spacer(Modifier.height(12.dp))
                            OutlinedDashBtn(onClick = onGoToAssignHomework, icon = Icons.AutoMirrored.Filled.Assignment, label = "ДОМАШНИ НА КЛАСА")
                            Spacer(Modifier.height(12.dp))
                            OutlinedDashBtn(onClick = onGoToTestManagement, icon = Icons.Default.Quiz, label = "УПРАВЛЕНИЕ НА ТЕСТОВЕ")
                        }

                        // ── EXPERT ────────────────────────────────
                        3 -> {
                            Text(
                                "Експертно табло",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))

                            // ← THIS BUTTON WAS MISSING
                            DashBtn(
                                onClick = onGoToExpertPanel,
                                icon = Icons.Default.AutoAwesome,
                                label = "AI ГЕНЕРАТОР НА УПРАЖНЕНИЯ"
                            )
                            Spacer(Modifier.height(12.dp))
                            DashBtn(
                                onClick = onGoToExpert,
                                icon = Icons.Default.PendingActions,
                                label = "ЧАКАЩИ ОДОБРЕНИЕ"
                            )
                            Spacer(Modifier.height(12.dp))
                            DashBtn(
                                onClick = onGoToExpertActive,
                                icon = Icons.AutoMirrored.Filled.List,
                                label = "АКТИВНИ УПРАЖНЕНИЯ",
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // ── STUDENT (default) ─────────────────────
                        else -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    "Ниво ${profile?.english_level ?: "A1"}  ·  ${profile?.total_xp} XP 🏆",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            DashBtn(onClick = onGoToPath, icon = Icons.Default.Map, label = "МОЯ УЧЕБЕН ПЪТ")
                            Spacer(Modifier.height(12.dp))
                            OutlinedDashBtn(onClick = onGoToMyClassrooms, icon = Icons.Default.School, label = "МОИТЕ КЛАСОВЕ")
                            Spacer(Modifier.height(12.dp))
                            OutlinedDashBtn(onClick = onGoToHomework, icon = Icons.AutoMirrored.Filled.Assignment, label = "ДОМАШНИ")
                            Spacer(Modifier.height(12.dp))
                            OutlinedDashBtn(onClick = onJoinClassroom, icon = Icons.Default.School, label = "ПРИСЪЕДИНИ СЕ КЪМ КЛАС")
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(onClick = onLogout) { Text("Изход от профила") }
                }
            }
        }
    }
}

// ── Small helpers to avoid repetition ────────────────────────────

@Composable
private fun DashBtn(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OutlinedDashBtn(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}