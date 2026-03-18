package com.viktor.englishapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
    // Инициализираме четеца на текст
    val tts = remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts.value?.language = java.util.Locale.US // Настройваме го на Американски Английски
            }
        }
        tts.value = textToSpeech
        onDispose { textToSpeech.shutdown() } // Спираме го, когато излезем от екрана
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasMicPermission = isGranted }
    )

    val exercise = remember {
        try {
            var cleanJson = exerciseJson.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()
            }
            Gson().fromJson(cleanJson, ExerciseContent::class.java)
        } catch (e: Exception) { null }
    }

    if (exercise == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Грешка при зареждане на упражнението.", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Назад") }
        }
        return
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = exercise.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = exercise.instructions, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(24.dp))

            // 🟢 ЕТО ГО РАЗДЕЛЕНИЕТО: ГОВОРЕНЕ ИЛИ ПИСАНЕ
            if (exercise.is_speaking) {

                // === ИЗГЛЕД ЗА ГОВОРЕНЕ (МИКРОФОН) ===
                exercise.content.forEachIndexed { index, questionText ->
                    Text(text = "${index + 1}. $questionText", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(32.dp))

                Text("Твоят устен отговор:", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            if (viewModel.isRecording) viewModel.stopRecording() else viewModel.startRecording(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.isRecording) Color.Red else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = if (viewModel.isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Микрофон", modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (viewModel.isRecording) "СПРИ ЗАПИСА" else "ЗАПОЧНИ ЗАПИС", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!viewModel.isRecording && !viewModel.isUploading && viewModel.evaluationResult == null) {
                    Button(
                        onClick = { val token = tokenManager.getToken(); if (token != null) viewModel.submitAudio(exerciseId, token) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("ИЗПРАТИ ЗА AI ОЦЕНКА") }
                }

                if (viewModel.isUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI анализира твоето произношение...")
                    }
                }

                viewModel.evaluationResult?.let { result ->
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Резултати от оценката", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Граматика: ${result.grammar_score}/100", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                            Text("Гладкост: ${result.fluency_score}/100", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1565C0))

                            Spacer(modifier = Modifier.height(16.dp))
                            // 🟢 НОВОТО: Текст + Бутон за слушане
                            Text("Как да го произнесеш по-добре:", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = result.pronunciation_tips,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        tts.value?.speak(result.pronunciation_tips, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Чуй произношението",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Силни страни:", fontWeight = FontWeight.Bold)
                            Text(result.strengths)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Слаби страни:", fontWeight = FontWeight.Bold, color = Color.Red)
                            Text(result.weaknesses)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Обяснение:", fontWeight = FontWeight.Bold)
                            Text(result.explanation)

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Това, което AI чу:", style = MaterialTheme.typography.labelMedium)
                            Text(result.transcribed_text, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }

            } else {
                // === ИЗГЛЕД ЗА ПИСАНЕ (ТЕКСТОВИ ПОЛЕТА) ===
                val userAnswers = remember { mutableStateListOf(*Array(exercise.content.size) { "" }) }
                var showResults by remember { mutableStateOf(false) }

                exercise.content.forEachIndexed { index, questionText ->
                    Text(text = "${index + 1}. $questionText", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userAnswers[index],
                        onValueChange = { userAnswers[index] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Твоят отговор") },
                        enabled = !showResults
                    )

                    if (showResults) {
                        Text(
                            text = "Правилен отговор: ${exercise.correct_answers[index]}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { if (showResults) onBack() else showResults = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (showResults) "ЗАТВОРИ" else "ПРОВЕРИ ОТГОВОРИТЕ")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}