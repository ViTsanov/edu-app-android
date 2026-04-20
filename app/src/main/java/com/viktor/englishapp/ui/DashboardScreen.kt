package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class DashboardViewModel : ViewModel() {

    var userProfile by mutableStateOf<UserProfile?>(null)
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun loadProfile(tokenManager: TokenManager) {
        val token = tokenManager.getToken()
        if (token == null) {
            errorMessage = "Липсва токен за достъп."
            isLoading = false
            return
        }
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

// ─────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToExpert: () -> Unit,
    onGoToExpertActive: () -> Unit,
    onGoToExercises: () -> Unit,
    onGoToClassrooms: () -> Unit,
    onCreateExercise: () -> Unit,
    onCreateTest: () -> Unit,
    onGoToPath: () -> Unit,
    onJoinClassroom: () -> Unit,
    onGoToMyClassrooms: () -> Unit,
    onGoToHomework: () -> Unit,
    onGoToAssignHomework: () -> Unit,
    onGoToTestManagement: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile(tokenManager)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EnglishApp") },
                actions = {
                    IconButton(onClick = onGoToProfile) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Профил",
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Зареждане на профила...")
                }

                viewModel.errorMessage.isNotEmpty() -> {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onLogout) { Text("Обратно към Вход") }
                }

                else -> {
                    val profile = viewModel.userProfile

                    Text(
                        text = "Здравей, ${profile?.username}! 👋",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    when (profile?.role_id) {

                        // ── TEACHER ───────────────────────────────
                        2 -> {
                            Text(
                                "Учителско табло",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onGoToClassrooms,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.School, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("МОИ КЛАСОВЕ")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onCreateExercise,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("СЪЗДАЙ НОВО УПРАЖНЕНИЕ")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onCreateTest,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("СЪЗДАЙ НОВ ТЕСТ")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onGoToExpertActive,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("АКТИВНИ УПРАЖНЕНИЯ")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onGoToAssignHomework,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("ДОМАШНИ НА КЛАСА")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onGoToTestManagement,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("УПРАВЛЕНИЕ НА ТЕСТОВЕ")
                            }
                        }

                        // ── EXPERT ────────────────────────────────
                        3 -> {
                            Text(
                                "Експертно табло",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onGoToExpert,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.PendingActions, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("ЧАКАЩИ ОДОБРЕНИЕ")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onGoToExpertActive,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("АКТИВНИ УПРАЖНЕНИЯ")
                            }
                        }

                        // ── STUDENT (default) ─────────────────────
                        else -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = "Ниво ${profile?.english_level ?: "A1"}  ·  ${profile?.total_xp} XP 🏆",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onGoToPath,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("МОЯ УЧЕБЕН ПЪТ")
                            }

                            Spacer(Modifier.height(12.dp))

                            // BUG FIX: Spacer and Text are now INSIDE the lambda
                            OutlinedButton(
                                onClick = onGoToMyClassrooms,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.School, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("МОИТЕ КЛАСОВЕ")
                            }

                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onGoToHomework,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("ДОМАШНИ")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onJoinClassroom,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.School, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("ПРИСЪЕДИНИ СЕ КЪМ КЛАС")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(onClick = onLogout) {
                        Text("Изход от профила")
                    }
                }
            }
        }
    }
}