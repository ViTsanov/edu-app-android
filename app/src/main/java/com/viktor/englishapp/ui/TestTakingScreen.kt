package com.viktor.englishapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.StudentPathItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────
data class TestExerciseItem(
    val testExerciseId: Int,
    val title: String,
    val contentPrompt: String,
    val source: String
)

data class TestInfo(
    val id: Int,
    val title: String,
    val timeLimitMinutes: Int,
    val exercises: List<TestExerciseItem>
)

// ── ViewModel ─────────────────────────────────────────────────────
class TestTakingViewModel : ViewModel() {
    var testInfo by mutableStateOf<TestInfo?>(null)
    var isLoading by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)
    var error by mutableStateOf("")
    var result by mutableStateOf<Map<String, Any>?>(null)

    // Timer
    var secondsRemaining by mutableStateOf(0)
    private var timerJob: Job? = null

    // Per-exercise user answers  key = testExerciseId
    val userAnswers = mutableMapOf<Int, List<String>>()

    fun loadTest(token: String, testId: Int) {
        if (testInfo != null) return
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.instance.getTestForStudent(
                    testId = testId,
                    token = "Bearer $token"
                )
                val exercises = (response["exercises"] as? List<*>)?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    TestExerciseItem(
                        testExerciseId = (map["test_exercise_id"] as? Double)?.toInt() ?: 0,
                        title = map["title"] as? String ?: "",
                        contentPrompt = map["content_prompt"] as? String ?: "",
                        source = map["source"] as? String ?: ""
                    )
                } ?: emptyList()

                val timeLimit = (response["time_limit_minutes"] as? Double)?.toInt() ?: 0
                testInfo = TestInfo(
                    id = testId,
                    title = response["title"] as? String ?: "",
                    timeLimitMinutes = timeLimit,
                    exercises = exercises
                )

                if (timeLimit > 0) {
                    secondsRemaining = timeLimit * 60
                    startTimer { submitTest(token, testId) }
                }
            } catch (e: Exception) {
                error = "Грешка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun startTimer(onExpire: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (secondsRemaining > 0) {
                delay(1000)
                secondsRemaining--
            }
            onExpire()
        }
    }

    fun submitTest(token: String, testId: Int) {
        timerJob?.cancel()
        viewModelScope.launch {
            isSubmitting = true
            try {
                val test = testInfo ?: return@launch
                val answers = test.exercises.mapIndexed { _, ex ->
                    val answers = userAnswers[ex.testExerciseId] ?: emptyList()
                    mapOf(
                        "test_exercise_id" to ex.testExerciseId,
                        "questions" to listOf(ex.title),
                        "expected_answers" to listOf(""),
                        "user_answers" to answers
                    )
                }
                result = RetrofitClient.instance.submitTest(
                    testId = testId,
                    body = mapOf("answers" to answers),
                    token = "Bearer $token"
                )
            } catch (e: Exception) {
                error = "Грешка при изпращане: ${e.message}"
            } finally {
                isSubmitting = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

// ── Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestTakingScreen(
    testId: Int,
    onBack: () -> Unit,
    viewModel: TestTakingViewModel = viewModel()
) {
    val context = LocalContext.current
    val token = remember { TokenManager(context).getToken() }
    var currentExerciseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(testId) {
        if (token != null) viewModel.loadTest(token, testId)
    }

    // Show results if submitted
    val result = viewModel.result
    if (result != null) {
        TestResultScreen(result = result, onBack = onBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.testInfo?.title ?: "Тест") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    // Countdown timer chip
                    if ((viewModel.testInfo?.timeLimitMinutes ?: 0) > 0) {
                        TimerChip(secondsRemaining = viewModel.secondsRemaining)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                viewModel.isSubmitting -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("AI анализира теста... може да отнеме 15-30 сек.")
                    }
                }
                viewModel.error.isNotEmpty() -> {
                    Text(
                        viewModel.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }
                viewModel.testInfo != null -> {
                    val test = viewModel.testInfo!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (currentExerciseIndex + 1).toFloat() / test.exercises.size.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Въпрос ${currentExerciseIndex + 1} от ${test.exercises.size}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // Animated exercise content
                        AnimatedContent(
                            targetState = currentExerciseIndex,
                            modifier = Modifier.weight(1f),
                            transitionSpec = {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            },
                            label = "exercise_slide"
                        ) { index ->
                            val exercise = test.exercises.getOrNull(index) ?: return@AnimatedContent
                            TestExercisePanel(
                                exercise = exercise,
                                existingAnswers = viewModel.userAnswers[exercise.testExerciseId] ?: emptyList(),
                                onAnswersChanged = { answers ->
                                    viewModel.userAnswers[exercise.testExerciseId] = answers
                                }
                            )
                        }

                        // Navigation buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (currentExerciseIndex > 0) {
                                OutlinedButton(
                                    onClick = { currentExerciseIndex-- },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Назад")
                                }
                            }

                            if (currentExerciseIndex < test.exercises.size - 1) {
                                Button(
                                    onClick = { currentExerciseIndex++ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Следващ →")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (token != null) viewModel.submitTest(token, test.id)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text("ПРЕДАЙ ТЕСТА", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerChip(secondsRemaining: Int) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val isUrgent = secondsRemaining <= 60

    val color by animateColorAsState(
        targetValue = if (isUrgent) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "timer_color"
    )

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TestExercisePanel(
    exercise: TestExerciseItem,
    existingAnswers: List<String>,
    onAnswersChanged: (List<String>) -> Unit
) {
    // Parse the exercise JSON
    val parsed = remember(exercise.contentPrompt) {
        try {
            com.google.gson.Gson().fromJson(
                exercise.contentPrompt,
                com.viktor.englishapp.domain.ExerciseContent::class.java
            )
        } catch (e: Exception) { null }
    }

    val questions = parsed?.content ?: emptyList()
    val answers = remember(existingAnswers, questions) {
        mutableStateListOf(*Array(questions.size) {
            existingAnswers.getOrElse(it) { "" }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(exercise.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            parsed?.instructions ?: "Отговорете на въпросите.",
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(24.dp))

        questions.forEachIndexed { index, question ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 80L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { 30 }) + fadeIn()
            ) {
                Column {
                    Text(
                        "${index + 1}. $question",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answers.getOrElse(index) { "" },
                        onValueChange = { newVal ->
                            if (index < answers.size) {
                                answers[index] = newVal
                                onAnswersChanged(answers.toList())
                            }
                        },
                        label = { Text("Твоят отговор") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TestResultScreen(result: Map<String, Any>, onBack: () -> Unit) {
    val score = (result["total_score"] as? Double)?.toInt() ?: 0
    val xp = (result["xp_earned"] as? Double)?.toInt() ?: 0
    val feedback = result["ai_feedback"] as? String ?: ""

    // Animated score reveal
    var displayedScore by remember { mutableStateOf(0) }
    LaunchedEffect(score) {
        val step = if (score > 20) score / 20 else 1
        while (displayedScore < score) {
            delay(40)
            displayedScore = minOf(displayedScore + step, score)
        }
    }

    val scoreColor = when {
        score >= 80 -> Color(0xFF16A34A)
        score >= 60 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Big animated score
        Text(
            text = "$displayedScore",
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColor
        )
        Text("/100", color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp)

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = displayedScore >= score, enter = fadeIn() + scaleIn()) {
            Surface(
                color = Color(0xFFFEF3C7),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "+$xp XP добавени!",
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // AI feedback card
        if (feedback.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI анализ на теста",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(feedback, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Към началото")
        }
    }
}