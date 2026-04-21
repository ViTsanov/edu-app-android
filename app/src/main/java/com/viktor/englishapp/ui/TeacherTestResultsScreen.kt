package com.viktor.englishapp.ui

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

// ─────────────────────────────────────────────────────────────────
// Data
// ─────────────────────────────────────────────────────────────────

data class TestAttemptResult(
    val studentId: Int,
    val studentName: String,
    val totalScore: Int?,
    val xpEarned: Int,
    val startedAt: String?,
    val finishedAt: String?,
    val durationSeconds: Int?,
    val aiFeedback: String?
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class TeacherTestResultsViewModel : ViewModel() {

    var results by mutableStateOf<List<TestAttemptResult>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager, testId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance
                    .getTestResults(testId, "Bearer $token")
                results = response.map { map ->
                    TestAttemptResult(
                        studentId = (map["student_id"] as? Double)?.toInt() ?: 0,
                        studentName = map["student_name"] as? String ?: "",
                        totalScore = (map["total_score"] as? Double)?.toInt(),
                        xpEarned = (map["xp_earned"] as? Double)?.toInt() ?: 0,
                        startedAt = map["started_at"] as? String,
                        finishedAt = map["finished_at"] as? String,
                        durationSeconds = (map["duration_seconds"] as? Double)?.toInt(),
                        aiFeedback = map["ai_feedback"] as? String
                    )
                }.sortedByDescending { it.totalScore ?: -1 }
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
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
fun TeacherTestResultsScreen(
    testId: Int,
    testTitle: String,
    onBack: () -> Unit,
    viewModel: TeacherTestResultsViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(testId) { viewModel.load(tokenManager, testId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(testTitle, style = MaterialTheme.typography.titleMedium)
                        if (!viewModel.isLoading) {
                            Text(
                                "${viewModel.results.size} резултата",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.errorMessage.isNotEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            viewModel.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HourglassEmpty, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Все още няма предадени тестове",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary card
                    item {
                        val avg = viewModel.results
                            .mapNotNull { it.totalScore }
                            .average()
                            .let { if (it.isNaN()) 0 else it.toInt() }
                        val best = viewModel.results.mapNotNull { it.totalScore }.maxOrNull() ?: 0
                        val avgDuration = viewModel.results
                            .mapNotNull { it.durationSeconds }
                            .average()
                            .let { if (it.isNaN()) 0 else it.toInt() }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ResultStat("${viewModel.results.size}", "Предали")
                                ResultStat("$avg/100", "Среден")
                                ResultStat("$best/100", "Най-добър")
                                ResultStat(formatDuration(avgDuration), "Ср. времe")
                            }
                        }
                    }

                    items(viewModel.results.withIndex().toList()) { (rank, result) ->
                        TestResultCard(rank = rank + 1, result = result)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Result card per student
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TestResultCard(rank: Int, result: TestAttemptResult) {
    var expanded by remember { mutableStateOf(false) }

    val score = result.totalScore ?: 0
    val scoreColor = when {
        score >= 80 -> Color(0xFF16A34A)
        score >= 60 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700) // gold
        2 -> Color(0xFFC0C0C0) // silver
        3 -> Color(0xFFCD7F32) // bronze
        else -> MaterialTheme.colorScheme.outline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank badge
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = medalColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = medalColor
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.studentName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    result.finishedAt?.let { at ->
                        Text(
                            "Предаден: ${at.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Score badge
                Surface(
                    color = scoreColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        "$score/100",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                result.durationSeconds?.let { dur ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            formatDuration(dur),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star, null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFFD97706)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "+${result.xpEarned} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706)
                    )
                }
            }

            // AI feedback toggle
            if (!result.aiFeedback.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (expanded) "▲ Скрий AI анализа" else "▼ Виж AI анализа",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (expanded) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            result.aiFeedback,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private fun formatDuration(seconds: Int): String = when {
    seconds >= 3600 -> "${seconds / 3600}ч ${(seconds % 3600) / 60}м"
    seconds >= 60   -> "${seconds / 60}м ${seconds % 60}с"
    else            -> "${seconds}с"
}