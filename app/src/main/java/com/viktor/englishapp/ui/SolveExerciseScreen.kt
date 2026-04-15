package com.viktor.englishapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ExerciseContent
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────
// Main screen composable
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveExerciseScreen(
    exerciseId: Int,
    exerciseJson: String,
    onBack: () -> Unit,
    viewModel: SolveExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // Text-to-speech setup
    val tts = remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts.value?.language = java.util.Locale.US
            }
        }
        tts.value = textToSpeech
        onDispose { textToSpeech.shutdown() }
    }

    // Microphone permission
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasMicPermission = isGranted }
    )

    // Parse the exercise JSON
    val exercise = remember {
        try {
            var cleanJson = exerciseJson.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()
            }
            Gson().fromJson(cleanJson, ExerciseContent::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Error state — exercise couldn't be parsed
    if (exercise == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Грешка при зареждане на упражнението.",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Назад") }
        }
        return
    }

    val safeContent = exercise.content ?: emptyList()
    val safeAnswers = exercise.correct_answers ?: emptyList()

    // ── Slide-in animation for entire exercise content ────────────
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        screenVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Упражнение №$exerciseId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = screenVisible,
            enter = slideInVertically(
                initialOffsetY = { 60 },
                animationSpec = tween(350, easing = EaseOut)
            ) + fadeIn(tween(350))
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Text(
                    text = exercise.title ?: "Без заглавие",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Instructions
                Text(
                    text = exercise.instructions ?: "Няма инструкции",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (exercise.is_speaking) {
                    // ═══════════════════════════════════════════════
                    // SPEAKING EXERCISE
                    // ═══════════════════════════════════════════════

                    // Staggered sentence list
                    safeContent.forEachIndexed { index, questionText ->
                        AnimatedSentenceItem(index = index, text = questionText)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        "Твоят устен отговор:",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated recording button with pulse
                    AnimatedRecordingButton(
                        isRecording = viewModel.isRecording,
                        hasMicPermission = hasMicPermission,
                        onStart = { viewModel.startRecording(context) },
                        onStop = { viewModel.stopRecording() },
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit audio button (shown only when not recording and not yet submitted)
                    if (!viewModel.isRecording &&
                        !viewModel.isUploading &&
                        viewModel.audioEvaluationResult == null
                    ) {
                        Button(
                            onClick = {
                                val token = tokenManager.getToken()
                                if (token != null) viewModel.submitAudio(exerciseId, token)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("ИЗПРАТИ ЗА AI ОЦЕНКА")
                        }
                    }

                    // Uploading indicator
                    if (viewModel.isUploading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI анализира твоето произношение...")
                        }
                    }

                    // Audio evaluation results
                    viewModel.audioEvaluationResult?.let { result ->
                        Spacer(modifier = Modifier.height(32.dp))
                        AnimatedAudioResultCard(
                            grammarScore = result.grammar_score,
                            fluencyScore = result.fluency_score,
                            pronunciationTips = result.pronunciation_tips,
                            strengths = result.strengths,
                            weaknesses = result.weaknesses,
                            explanation = result.explanation,
                            onSpeakTip = { text ->
                                tts.value?.speak(
                                    text,
                                    android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        )
                    }

                } else {
                    // ═══════════════════════════════════════════════
                    // TEXT / WRITING EXERCISE
                    // ═══════════════════════════════════════════════

                    val userAnswers = remember {
                        mutableStateListOf(*Array(safeContent.size) { "" })
                    }

                    if (safeContent.isEmpty()) {
                        Text(
                            "AI не е генерирал въпроси за това упражнение.",
                            color = Color.Red
                        )
                    } else {
                        // Staggered animated question list
                        safeContent.forEachIndexed { index, questionText ->
                            val isCorrect =
                                viewModel.textEvaluationResult?.is_correct_array?.getOrNull(index)
                            AnimatedQuestionItem(
                                index = index,
                                questionText = questionText,
                                answer = userAnswers.getOrElse(index) { "" },
                                isCorrect = isCorrect,
                                correctAnswer = safeAnswers.getOrNull(index) ?: "",
                                enabled = viewModel.textEvaluationResult == null &&
                                        !viewModel.isEvaluatingText,
                                onAnswerChange = { newVal ->
                                    if (index < userAnswers.size) userAnswers[index] = newVal
                                }
                            )
                        }

                        // Submit button
                        if (viewModel.textEvaluationResult == null) {
                            Button(
                                onClick = {
                                    val token = tokenManager.getToken()
                                    if (token != null) {
                                        viewModel.submitTextExercise(
                                            exerciseId = exerciseId,
                                            questions = safeContent,
                                            expectedAnswers = safeAnswers,
                                            userAnswers = userAnswers.toList(),
                                            token = token
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = !viewModel.isEvaluatingText
                            ) {
                                if (viewModel.isEvaluatingText) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("ПРОВЕРИ ОТГОВОРИТЕ С AI")
                                }
                            }
                        }
                    }

                    // Animated text evaluation results
                    viewModel.textEvaluationResult?.let { result ->
                        Spacer(modifier = Modifier.height(32.dp))
                        AnimatedTextResultCard(
                            grammarScore = result.grammar_score,
                            xpEarned = result.xp_earned,
                            strengths = result.strengths,
                            weaknesses = result.weaknesses,
                            explanation = result.explanation
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            } // end Column
        } // end AnimatedVisibility
    } // end Scaffold
}

// ─────────────────────────────────────────────────────────────────
// Animated sentence item (speaking exercise sentence list)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedSentenceItem(index: Int, text: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -40 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Column {
            Text(
                text = "${index + 1}. $text",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Animated question + answer field (writing exercise)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedQuestionItem(
    index: Int,
    questionText: String,
    answer: String,
    isCorrect: Boolean?,
    correctAnswer: String,
    enabled: Boolean,
    onAnswerChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 100L)   // each question appears 100 ms after the previous one
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(tween(300))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${index + 1}. $questionText",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Твоят отговор") },
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = when (isCorrect) {
                        true -> Color(0xFF16A34A)
                        false -> Color.Red
                        null -> Color.Black
                    },
                    disabledBorderColor = when (isCorrect) {
                        true -> Color(0xFF16A34A)
                        false -> Color.Red
                        null -> Color.Gray
                    }
                )
            )

            // Correct answer hint (shown only for wrong answers after evaluation)
            AnimatedVisibility(visible = isCorrect == false) {
                Text(
                    text = "✓ Правилен: $correctAnswer",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Animated recording button with pulse ring
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedRecordingButton(
    isRecording: Boolean,
    hasMicPermission: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pulsing halo — only drawn while recording
        if (isRecording) {
            Surface(
                modifier = Modifier.size((72 * pulseScale).dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Red.copy(alpha = pulseAlpha)
            ) {}
        }

        Button(
            onClick = {
                when {
                    !hasMicPermission -> onRequestPermission()
                    isRecording -> onStop()
                    else -> onStart()
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red
                else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Микрофон",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isRecording) "СПРИ ЗАПИСА" else "ЗАПОЧНИ ЗАПИС",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Animated audio result card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedAudioResultCard(
    grammarScore: Int,
    fluencyScore: Int,
    pronunciationTips: String?,
    strengths: String?,
    weaknesses: String?,
    explanation: String?,
    onSpeakTip: (String) -> Unit
) {
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        cardVisible = true
    }

    // Count-up for grammar score
    var displayGrammar by remember { mutableStateOf(0) }
    var displayFluency by remember { mutableStateOf(0) }
    LaunchedEffect(grammarScore) {
        delay(400)
        while (displayGrammar < grammarScore) {
            delay(18)
            displayGrammar = minOf(displayGrammar + 2, grammarScore)
        }
    }
    LaunchedEffect(fluencyScore) {
        delay(600)
        while (displayFluency < fluencyScore) {
            delay(18)
            displayFluency = minOf(displayFluency + 2, fluencyScore)
        }
    }

    val scoreColor by animateColorAsState(
        targetValue = when {
            grammarScore >= 80 -> Color(0xFF16A34A)
            grammarScore >= 60 -> Color(0xFFD97706)
            else -> Color(0xFFDC2626)
        },
        animationSpec = tween(600),
        label = "score_color"
    )

    AnimatedVisibility(
        visible = cardVisible,
        enter = slideInVertically(initialOffsetY = { 80 }) + fadeIn(tween(400))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Резултати от оценката",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Animated scores
                Text(
                    "Граматика: $displayGrammar/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = scoreColor
                )
                Text(
                    "Гладкост: $displayFluency/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1565C0)
                )

                if (!pronunciationTips.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Как да го произнесеш по-добре:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pronunciationTips,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onSpeakTip(pronunciationTips) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Чуй произношението",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!strengths.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Силни страни:", fontWeight = FontWeight.Bold)
                    Text(strengths)
                }

                if (!weaknesses.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Слаби страни:", fontWeight = FontWeight.Bold, color = Color.Red)
                    Text(weaknesses)
                }

                if (!explanation.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Обяснение:", fontWeight = FontWeight.Bold)
                    Text(explanation)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Animated text result card (count-up score + XP pop-in)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedTextResultCard(
    grammarScore: Int,
    xpEarned: Int?,
    strengths: String?,
    weaknesses: String?,
    explanation: String?
) {
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        cardVisible = true
    }

    // Count-up animation
    var displayScore by remember { mutableStateOf(0) }
    LaunchedEffect(grammarScore) {
        delay(350)
        while (displayScore < grammarScore) {
            delay(18)
            displayScore = minOf(displayScore + 2, grammarScore)
        }
    }

    val scoreColor by animateColorAsState(
        targetValue = when {
            grammarScore >= 80 -> Color(0xFF16A34A)
            grammarScore >= 60 -> Color(0xFFD97706)
            else -> Color(0xFFDC2626)
        },
        animationSpec = tween(600),
        label = "score_color"
    )

    AnimatedVisibility(
        visible = cardVisible,
        enter = slideInVertically(initialOffsetY = { 80 }) + fadeIn(tween(400))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // XP badge — pops in after count-up finishes
                xpEarned?.let { xp ->
                    AnimatedVisibility(
                        visible = displayScore >= grammarScore,
                        enter = scaleIn(
                            spring(Spring.DampingRatioMediumBouncy)
                        ) + fadeIn()
                    ) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🏆 +$xp Точки (XP) добавени към профила ти!",
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Score header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Резултати и Оценка от AI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$displayScore/100",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!strengths.isNullOrEmpty()) {
                    Text("Силни страни:", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    Text(strengths)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!weaknesses.isNullOrEmpty()) {
                    Text("Къде да внимаваш:", fontWeight = FontWeight.Bold, color = Color.Red)
                    Text(weaknesses)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!explanation.isNullOrEmpty()) {
                    Text("Подробно обяснение от учителя:", fontWeight = FontWeight.Bold)
                    Text(explanation)
                }
            }
        }
    }
}