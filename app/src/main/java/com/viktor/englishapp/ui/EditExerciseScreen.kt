package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.viktor.englishapp.domain.ExerciseUpdateRequest
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class EditExerciseViewModel : ViewModel() {

    // Parsed fields
    var title by mutableStateOf("")
    var instructions by mutableStateOf("")
    var isSpeaking by mutableStateOf(false)
    val questions = mutableStateListOf<String>()
    val answers = mutableStateListOf<String>()

    // Fallback when JSON cannot be parsed
    var parseError by mutableStateOf(false)
    var rawFallbackContent by mutableStateOf("")

    // UI state
    var isSaving by mutableStateOf(false)
    var isApproving by mutableStateOf(false)
    var statusMessage by mutableStateOf("")
    var isError by mutableStateOf(false)

    /** Parses the initial JSON content into editable fields. */
    fun parseContent(initialContent: String) {
        rawFallbackContent = initialContent
        try {
            var cleanJson = initialContent.trim()

            // Auto-decode Base64 if needed
            if (!cleanJson.startsWith("{")) {
                try {
                    val decoded = android.util.Base64.decode(
                        cleanJson,
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                    )
                    cleanJson = String(decoded, Charsets.UTF_8).trim()
                } catch (_: Exception) { /* not Base64, continue */ }
            }

            // Strip Markdown fences
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()
            }

            rawFallbackContent = cleanJson

            val parsed = Gson().fromJson(cleanJson, ExerciseContent::class.java)
            if (parsed != null) {
                title = parsed.title ?: ""
                instructions = parsed.instructions ?: ""
                isSpeaking = parsed.is_speaking
                parsed.content?.let { questions.addAll(it) }
                parsed.correct_answers?.let { answers.addAll(it) }
                parseError = false
            } else {
                parseError = true
            }
        } catch (_: Exception) {
            parseError = true
        }
    }

    /** Saves edits (and optionally approves) the exercise. */
    fun saveExercise(
        exerciseId: Int,
        tokenManager: TokenManager,
        andApprove: Boolean,
        onApproved: () -> Unit
    ) {
        val token = tokenManager.getToken() ?: return

        if (andApprove) isApproving = true else isSaving = true
        statusMessage = ""
        isError = false

        viewModelScope.launch {
            try {
                val updatedJson = if (parseError) {
                    rawFallbackContent
                } else {
                    Gson().toJson(
                        ExerciseContent(
                            title = title,
                            instructions = instructions,
                            is_speaking = isSpeaking,
                            content = questions.toList(),
                            correct_answers = answers.toList()
                        )
                    )
                }

                RetrofitClient.instance.editExercise(
                    exerciseId = exerciseId,
                    request = ExerciseUpdateRequest(content_prompt = updatedJson),
                    token = "Bearer $token"
                )

                if (andApprove) {
                    RetrofitClient.instance.approveExercise(
                        exerciseId = exerciseId,
                        token = "Bearer $token"
                    )
                    statusMessage = "Успешно одобрено!"
                    onApproved()
                } else {
                    statusMessage = "Промените са запазени!"
                }
            } catch (e: Exception) {
                statusMessage = "Грешка: ${e.message}"
                isError = true
            } finally {
                isSaving = false
                isApproving = false
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    exerciseId: Int,
    exerciseTitle: String,
    initialContent: String,
    onBack: () -> Unit,
    onApproved: () -> Unit,
    viewModel: EditExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    // Parse content exactly once when the screen opens
    LaunchedEffect(initialContent) {
        viewModel.parseContent(initialContent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редакция: $exerciseTitle") },
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
            if (viewModel.parseError) {
                // Fallback: raw text editor
                Text(
                    "AI върна неформатиран текст. Редактирайте суровия JSON:",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.rawFallbackContent,
                    onValueChange = { viewModel.rawFallbackContent = it },
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            } else {
                // Structured editor
                Text(
                    "Основна информация",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text("Заглавие") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.instructions,
                    onValueChange = { viewModel.instructions = it },
                    label = { Text("Инструкции за ученика") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    if (viewModel.isSpeaking) "Текстове за говорене" else "Въпроси и Отговори",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                viewModel.questions.forEachIndexed { index, _ ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Въпрос ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = viewModel.questions[index],
                                onValueChange = { viewModel.questions[index] = it },
                                label = {
                                    Text(
                                        if (viewModel.isSpeaking) "Изречение за произнасяне"
                                        else "Въпрос / Изречение"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!viewModel.isSpeaking && index < viewModel.answers.size) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = viewModel.answers[index],
                                    onValueChange = { viewModel.answers[index] = it },
                                    label = { Text("Правилен отговор") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Status message
            if (viewModel.statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = viewModel.statusMessage,
                    color = if (viewModel.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.saveExercise(
                            exerciseId = exerciseId,
                            tokenManager = tokenManager,
                            andApprove = false,
                            onApproved = {}
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.isSaving && !viewModel.isApproving
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Запази")
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveExercise(
                            exerciseId = exerciseId,
                            tokenManager = tokenManager,
                            andApprove = true,
                            onApproved = onApproved
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !viewModel.isSaving && !viewModel.isApproving
                ) {
                    if (viewModel.isApproving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Запази и Одобри")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}