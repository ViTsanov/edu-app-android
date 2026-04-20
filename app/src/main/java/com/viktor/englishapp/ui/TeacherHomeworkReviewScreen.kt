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
import com.google.gson.Gson
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ExerciseContent
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────

data class HomeworkStudentSubmission(
    val studentId: Int,
    val username: String,
    val submitted: Boolean,
    val submittedAt: String?,
    val userAnswers: List<String>,
    val grammarScore: Int?,
    val fluencyScore: Int?,
    val strengths: String,
    val weaknesses: String,
    val explanation: String,
    val xpEarned: Int
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class TeacherHomeworkReviewViewModel : ViewModel() {

    var submissions by mutableStateOf<List<HomeworkStudentSubmission>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")
    var homeworkTitle by mutableStateOf("")

    // The exercise content for showing questions alongside answers
    var exerciseContent by mutableStateOf<ExerciseContent?>(null)

    fun load(tokenManager: TokenManager, homeworkId: Int, contentPrompt: String?) {
        // Parse content prompt to get questions
        contentPrompt?.let {
            try {
                exerciseContent = Gson().fromJson(it, ExerciseContent::class.java)
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance
                    .getHomeworkSubmissions(homeworkId, "Bearer $token")

                submissions = response.map { map ->
                    @Suppress("UNCHECKED_CAST")
                    val answers = (map["user_answers"] as? List<String>) ?: emptyList()
                    HomeworkStudentSubmission(
                        studentId = (map["student_id"] as? Double)?.toInt() ?: 0,
                        username = map["username"] as? String ?: "",
                        submitted = map["submitted"] as? Boolean ?: false,
                        submittedAt = map["submitted_at"] as? String,
                        userAnswers = answers,
                        grammarScore = (map["grammar_score"] as? Double)?.toInt(),
                        fluencyScore = (map["fluency_score"] as? Double)?.toInt(),
                        strengths = map["strengths"] as? String ?: "",
                        weaknesses = map["weaknesses"] as? String ?: "",
                        explanation = map["explanation"] as? String ?: "",
                        xpEarned = (map["xp_earned"] as? Double)?.toInt() ?: 0
                    )
                }
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
fun TeacherHomeworkReviewScreen(
    homeworkId: Int,
    homeworkTitle: String,
    contentPrompt: String?,
    onBack: () -> Unit,
    viewModel: TeacherHomeworkReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(homeworkId) {
        viewModel.load(tokenManager, homeworkId, contentPrompt)
    }

    val submitted = viewModel.submissions.count { it.submitted }
    val total = viewModel.submissions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(homeworkTitle, style = MaterialTheme.typography.titleMedium)
                        if (!viewModel.isLoading) {
                            Text(
                                "$submitted / $total предали",
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
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    item {
                        SubmissionSummaryCard(
                            submitted = submitted,
                            total = total,
                            avgScore = if (submitted > 0)
                                viewModel.submissions
                                    .filter { it.submitted }
                                    .mapNotNull { it.grammarScore }
                                    .average().toInt()
                            else 0
                        )
                    }

                    // Student submissions
                    items(viewModel.submissions) { sub ->
                        StudentSubmissionCard(
                            submission = sub,
                            questions = viewModel.exerciseContent?.content ?: emptyList(),
                            correctAnswers = viewModel.exerciseContent?.correct_answers ?: emptyList()
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Summary card at top
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SubmissionSummaryCard(submitted: Int, total: Int, avgScore: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryMetric("$submitted/$total", "Предали")
            SummaryMetric("${total - submitted}", "Чакащи")
            SummaryMetric(if (submitted > 0) "$avgScore/100" else "—", "Среден резултат")
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Per-student card — expandable
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StudentSubmissionCard(
    submission: HomeworkStudentSubmission,
    questions: List<String>,
    correctAnswers: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = when {
        !submission.submitted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        (submission.grammarScore ?: 0) >= 70 -> Color(0xFF16A34A)
        else -> Color(0xFFD97706)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !submission.submitted -> MaterialTheme.colorScheme.surfaceVariant
                (submission.grammarScore ?: 0) >= 70 -> Color(0xFFF0FDF4)
                else -> Color(0xFFFFF8F0)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header row — always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (submission.submitted)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            submission.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        submission.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (!submission.submitted) {
                        Text(
                            "Не е предал",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            "Предадено: ${submission.submittedAt?.take(10) ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (submission.submitted) {
                    // Score badge
                    val score = submission.grammarScore ?: 0
                    Surface(
                        color = if (score >= 70) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            "$score/100",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (score >= 70) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Expand toggle
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded details
            if (expanded && submission.submitted) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Q&A review
                questions.forEachIndexed { index, question ->
                    val userAnswer = submission.userAnswers.getOrElse(index) { "" }
                    val correct = correctAnswers.getOrElse(index) { "" }
                    val isCorrect = userAnswer.trim().equals(correct.trim(), ignoreCase = true)

                    QuestionAnswerRow(
                        index = index,
                        question = question,
                        userAnswer = userAnswer,
                        correctAnswer = correct,
                        isCorrect = isCorrect
                    )
                    if (index < questions.size - 1) {
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // AI feedback
                if (submission.explanation.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "AI Обяснение",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        submission.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (submission.strengths.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.ThumbUp, null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            submission.strengths,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (submission.weaknesses.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            submission.weaknesses,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Question / answer comparison row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun QuestionAnswerRow(
    index: Int,
    question: String,
    userAnswer: String,
    correctAnswer: String,
    isCorrect: Boolean
) {
    Surface(
        color = if (isCorrect) Color(0xFFE8FAF0) else Color(0xFFFFF3F3),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${index + 1}. $question",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    null,
                    tint = if (isCorrect) Color(0xFF16A34A) else Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Отговор: ${userAnswer.ifEmpty { "(без отговор)" }}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCorrect) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
            if (!isCorrect && correctAnswer.isNotEmpty()) {
                Text(
                    "Правилен: $correctAnswer",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}