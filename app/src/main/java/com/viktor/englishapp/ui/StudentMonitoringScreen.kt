package com.viktor.englishapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch

data class StudentStat(
    val studentId: Int,
    val username: String,
    val englishLevel: String,
    val totalXp: Int,
    val exercisesCompleted: Int,
    val averageScore: Int,
    val totalTimeSeconds: Int,
    val testsCompleted: Int
)

class MonitoringViewModel : ViewModel() {
    var stats by mutableStateOf<List<StudentStat>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf("")
    var selectedStudent by mutableStateOf<StudentStat?>(null)

    fun loadMonitoring(token: String, classroomId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.instance.getClassroomMonitoring(
                    classroomId = classroomId,
                    token = "Bearer $token"
                )
                stats = response.map { map ->
                    StudentStat(
                        studentId = (map["student_id"] as? Double)?.toInt() ?: 0,
                        username = map["username"] as? String ?: "",
                        englishLevel = map["english_level"] as? String ?: "A1",
                        totalXp = (map["total_xp"] as? Double)?.toInt() ?: 0,
                        exercisesCompleted = (map["exercises_completed"] as? Double)?.toInt() ?: 0,
                        averageScore = (map["average_score"] as? Double)?.toInt() ?: 0,
                        totalTimeSeconds = (map["total_time_seconds"] as? Double)?.toInt() ?: 0,
                        testsCompleted = (map["tests_completed"] as? Double)?.toInt() ?: 0
                    )
                }.sortedByDescending { it.averageScore }
            } catch (e: Exception) {
                error = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMonitoringScreen(
    classroomId: Int,
    onBack: () -> Unit,
    viewModel: MonitoringViewModel = viewModel()
) {
    val context = LocalContext.current
    val token = remember { TokenManager(context).getToken() }

    LaunchedEffect(classroomId) {
        if (token != null) viewModel.loadMonitoring(token, classroomId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мониторинг на класа") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                viewModel.error.isNotEmpty() -> Text(
                    viewModel.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                viewModel.stats.isEmpty() -> Text(
                    "Няма ученици в класа.",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Class summary header
                        item { ClassSummaryCard(stats = viewModel.stats) }

                        item {
                            Text(
                                "Детайли по ученик",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(viewModel.stats) { stat ->
                            StudentStatCard(
                                stat = stat,
                                onClick = { viewModel.selectedStudent = stat }
                            )
                        }
                    }

                    // Student detail dialog
                    viewModel.selectedStudent?.let { student ->
                        StudentDetailDialog(
                            student = student,
                            onDismiss = { viewModel.selectedStudent = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassSummaryCard(stats: List<StudentStat>) {
    val avgScore = if (stats.isEmpty()) 0 else stats.sumOf { it.averageScore } / stats.size
    val totalExercises = stats.sumOf { it.exercisesCompleted }
    val totalTimeHours = stats.sumOf { it.totalTimeSeconds } / 3600

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Обобщение на класа",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric("${stats.size}", "Ученика")
                SummaryMetric("$avgScore%", "Среден резултат")
                SummaryMetric("$totalExercises", "Упражнения")
                SummaryMetric("${totalTimeHours}ч", "Общо в апп")
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun StudentStatCard(stat: StudentStat, onClick: () -> Unit) {
    val scoreColor = when {
        stat.averageScore >= 80 -> Color(0xFF16A34A)
        stat.averageScore >= 60 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.error
    }

    // Animated score bar
    val animatedScore by animateFloatAsState(
        targetValue = stat.averageScore / 100f,
        animationSpec = tween(durationMillis = 800, easing = EaseOut),
        label = "score_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circle
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            stat.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stat.username, fontWeight = FontWeight.Bold)
                    Text(
                        "Ниво ${stat.englishLevel} · ${stat.totalXp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    "${stat.averageScore}%",
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(12.dp))

            // Animated progress bar
            LinearProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier.fillMaxWidth(),
                color = scoreColor,
                trackColor = scoreColor.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStat(Icons.Default.Assignment, "${stat.exercisesCompleted} упражнения")
                MiniStat(Icons.Default.Quiz, "${stat.testsCompleted} теста")
                MiniStat(Icons.Default.AccessTime, formatTime(stat.totalTimeSeconds))
            }
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

private fun formatTime(seconds: Int): String {
    return when {
        seconds >= 3600 -> "${seconds / 3600}ч ${(seconds % 3600) / 60}м"
        seconds >= 60 -> "${seconds / 60}м"
        else -> "${seconds}с"
    }
}

@Composable
private fun StudentDetailDialog(student: StudentStat, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.username) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("Ниво", student.englishLevel)
                DetailRow("Общо XP", "${student.totalXp}")
                DetailRow("Упражнения", "${student.exercisesCompleted}")
                DetailRow("Тестове", "${student.testsCompleted}")
                DetailRow("Среден резултат", "${student.averageScore}%")
                DetailRow("Прекарано в апп", formatTime(student.totalTimeSeconds))

                val scoreColor = when {
                    student.averageScore >= 80 -> Color(0xFF16A34A)
                    student.averageScore >= 60 -> Color(0xFFD97706)
                    else -> MaterialTheme.colorScheme.error
                }
                val status = when {
                    student.averageScore >= 80 -> "Отличен напредък! 🏆"
                    student.averageScore >= 60 -> "Добро представяне, има място за подобрение."
                    student.exercisesCompleted == 0 -> "Не е правил упражнения още."
                    else -> "Нуждае се от допълнителна помощ."
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = scoreColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        status, color = scoreColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Затвори") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.secondary)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}