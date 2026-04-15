package com.viktor.englishapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.StudentPathItem
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class ExerciseListViewModel : ViewModel() {

    var exercises by mutableStateOf<List<StudentPathItem>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun loadPath(tokenManager: TokenManager) {
        val token = tokenManager.getToken()
        if (token == null) {
            errorMessage = "Липсва токен."
            isLoading = false
            return
        }
        viewModelScope.launch {
            try {
                exercises = RetrofitClient.instance.getMyPath("Bearer $token")
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
fun ExerciseListScreen(
    onBack: () -> Unit,
    onExerciseClick: (StudentPathItem) -> Unit,
    viewModel: ExerciseListViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) {
        viewModel.loadPath(tokenManager)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моят учебен път") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            viewModel.errorMessage.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            viewModel.exercises.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Няма налични упражнения за твоето ниво.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.exercises) { exercise ->
                        StudentPathCard(
                            exercise = exercise,
                            onClick = {
                                if (exercise.status != "COMPLETED") {
                                    onExerciseClick(exercise)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Exercise card (unchanged visual logic)
// ─────────────────────────────────────────────────────────────────

@Composable
fun StudentPathCard(exercise: StudentPathItem, onClick: () -> Unit) {
    val containerColor = when (exercise.status) {
        "COMPLETED" -> Color(0xFFF0FDF4)
        "RETRY"     -> Color(0xFFFFFBEB)
        else        -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusColor = when (exercise.status) {
        "COMPLETED" -> Color(0xFF16A34A)
        "RETRY"     -> Color(0xFFD97706)
        else        -> MaterialTheme.colorScheme.primary
    }
    val icon = when (exercise.status) {
        "COMPLETED" -> Icons.Default.CheckCircle
        "RETRY"     -> Icons.Default.Refresh
        else        -> Icons.Default.PlayArrow
    }
    val statusText = when (exercise.status) {
        "COMPLETED" -> "Завършено успешно (${exercise.best_score}/100)"
        "RETRY"     -> "Опитай пак (Най-добър резултат: ${exercise.best_score}/100)"
        else        -> "Ново упражнение"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = exercise.status != "COMPLETED") { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}