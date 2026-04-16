package com.viktor.englishapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
// Data model
// ─────────────────────────────────────────────────────────────────

data class ExerciseResultData(
    val exerciseId: Int,
    val exerciseTitle: String,
    val exerciseContent: ExerciseContent?,
    val userAnswers: List<String>,
    val xpEarned: Int,
    val completedAt: String?,
    val grammarScore: Int,
    val fluencyScore: Int,
    val strengths: String,
    val weaknesses: String,
    val explanation: String,
    val pronunciationTips: String?
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class ExerciseResultViewModel : ViewModel() {

    var result by mutableStateOf<ExerciseResultData?>(null)
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager, exerciseId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance.getMyExerciseResult(
                    exerciseId = exerciseId,
                    token = "Bearer $token"
                )

                // Parse the exercise content JSON
                val contentJson = response["exercise_content"] as? String
                val parsed = contentJson?.let {
                    try { Gson().fromJson(it, ExerciseContent::class.java) }
                    catch (_: Exception) { null }
                }

                // Parse user answers
                @Suppress("UNCHECKED_CAST")
                val answers = (response["user_answers"] as? List<String>) ?: emptyList()

                result = ExerciseResultData(
                    exerciseId = exerciseId,
                    exerciseTitle = response["exercise_title"] as? String ?: "",
                    exerciseContent = parsed,
                    userAnswers = answers,
                    xpEarned = (response["xp_earned"] as? Double)?.toInt() ?: 0,
                    completedAt = response["completed_at"] as? String,
                    grammarScore = (response["grammar_score"] as? Double)?.toInt() ?: 0,
                    fluencyScore = (response["fluency_score"] as? Double)?.toInt() ?: 0,
                    strengths = response["strengths"] as? String ?: "",
                    weaknesses = response["weaknesses"] as? String ?: "",
                    explanation = response["explanation"] as? String ?: "",
                    pronunciationTips = response["pronunciation_tips"] as? String
                )
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
fun ExerciseResultScreen(
    exerciseId: Int,
    onBack: () -> Unit,
    viewModel: ExerciseResultViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(exerciseId) { viewModel.load(tokenManager, exerciseId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Преглед на резултата") },
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
            viewModel.result != null -> {
                ResultContent(
                    result = viewModel.result!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main content
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ResultContent(result: ExerciseResultData, modifier: Modifier) {
    val scoreColor = when {
        result.grammarScore >= 80 -> Color(0xFF16A34A)
        result.grammarScore >= 60 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    // Animated score
    var displayScore by remember { mutableIntStateOf(0) }
    LaunchedEffect(result.grammarScore) {
        kotlinx.coroutines.delay(300)
        while (displayScore < result.grammarScore) {
            kotlinx.coroutines.delay(16)
            displayScore = minOf(displayScore + 2, result.grammarScore)
        }
    }

    // Animated progress bar
    val animProgress by animateFloatAsState(
        targetValue = result.grammarScore / 100f,
        animationSpec = tween(1000, easing = EaseOut),
        label = "score_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Score header ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = scoreColor.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    result.exerciseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "$displayScore",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    "/ 100",
                    style = MaterialTheme.typography.titleMedium,
                    color = scoreColor.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                    color = scoreColor,
                    trackColor = scoreColor.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            "+${result.xpEarned} XP",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                    if (result.fluencyScore > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text(
                                "Гладкост: ${result.fluencyScore}/100",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Questions and answers ─────────────────────────────────
        val questions = result.exerciseContent?.content ?: emptyList()
        val correctAnswers = result.exerciseContent?.correct_answers ?: emptyList()
        val isSpeaking = result.exerciseContent?.is_speaking ?: false

        if (questions.isNotEmpty()) {
            Text(
                if (isSpeaking) "Изречения за произнасяне" else "Въпроси и отговори",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            questions.forEachIndexed { index, question ->
                val userAnswer = result.userAnswers.getOrElse(index) { "" }
                val correctAnswer = correctAnswers.getOrElse(index) { "" }

                // Determine if correct (case-insensitive, trimmed)
                val isCorrect = if (isSpeaking) null
                else userAnswer.trim().equals(correctAnswer.trim(), ignoreCase = true)

                QuestionReviewCard(
                    index = index,
                    question = question,
                    userAnswer = userAnswer,
                    correctAnswer = correctAnswer,
                    isCorrect = isCorrect,
                    isSpeaking = isSpeaking
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── AI Analysis card ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "AI Анализ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))

                if (result.strengths.isNotEmpty()) {
                    AnalysisSection(
                        icon = Icons.Default.ThumbUp,
                        label = "Силни страни",
                        text = result.strengths,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (result.weaknesses.isNotEmpty()) {
                    AnalysisSection(
                        icon = Icons.Default.Warning,
                        label = "Слаби страни",
                        text = result.weaknesses,
                        color = Color(0xFFD97706)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (result.explanation.isNotEmpty()) {
                    AnalysisSection(
                        icon = Icons.Default.Info,
                        label = "Обяснение",
                        text = result.explanation,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!result.pronunciationTips.isNullOrEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    AnalysisSection(
                        icon = Icons.Default.RecordVoiceOver,
                        label = "Произношение",
                        text = result.pronunciationTips,
                        color = Color(0xFF6B21A8)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────
// Individual question review card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun QuestionReviewCard(
    index: Int,
    question: String,
    userAnswer: String,
    correctAnswer: String,
    isCorrect: Boolean?,  // null = speaking (no right/wrong)
    isSpeaking: Boolean
) {
    val bgColor = when (isCorrect) {
        true -> Color(0xFFF0FDF4)
        false -> Color(0xFFFFF7F7)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when (isCorrect) {
        true -> Color(0xFF16A34A)
        false -> Color(0xFFDC2626)
        null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Question number + status icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${index + 1}. $question",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                when (isCorrect) {
                    true -> Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                    false -> Icon(
                        Icons.Default.Cancel,
                        null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    null -> {}
                }
            }

            Spacer(Modifier.height(8.dp))

            // User's answer
            Surface(
                color = if (isCorrect == false) Color(0xFFFFEBEB)
                else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Твоят отговор: ${userAnswer.ifEmpty { "(без отговор)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCorrect == false) Color(0xFFDC2626)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Correct answer (shown when wrong, or always for speaking)
            if (isCorrect == false && correctAnswer.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFE8FAF0),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
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
    }
}

// ─────────────────────────────────────────────────────────────────
// Analysis section row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}