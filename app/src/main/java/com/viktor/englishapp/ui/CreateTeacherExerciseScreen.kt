package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.google.gson.Gson
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ExerciseContent
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class CreateTeacherExerciseViewModel : ViewModel() {

    var title by mutableStateOf("")
    var instructions by mutableStateOf("")
    var isSpeaking by mutableStateOf(false)
    val questions = mutableStateListOf("") // start with one empty question
    val answers = mutableStateListOf("")   // matching answers

    var isSaving by mutableStateOf(false)
    var successMessage by mutableStateOf("")
    var errorMessage by mutableStateOf("")

    fun addQuestion() {
        questions.add("")
        answers.add("")
    }

    fun removeQuestion(index: Int) {
        if (questions.size > 1) {
            questions.removeAt(index)
            answers.removeAt(index)
        }
    }

    fun saveExercise(tokenManager: TokenManager, onSuccess: () -> Unit) {
        if (title.isBlank()) {
            errorMessage = "Моля, въведете заглавие."
            return
        }
        if (questions.all { it.isBlank() }) {
            errorMessage = "Моля, добавете поне един въпрос."
            return
        }

        isSaving = true
        errorMessage = ""
        successMessage = ""

        val content = ExerciseContent(
            title = title,
            instructions = instructions,
            is_speaking = isSpeaking,
            content = questions.toList(),
            correct_answers = if (isSpeaking) emptyList() else answers.toList()
        )
        val contentJson = Gson().toJson(content)

        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: throw Exception("Липсва токен.")
                RetrofitClient.instance.saveTeacherExercise(
                    token = "Bearer $token",
                    body = mapOf(
                        "title" to title,
                        "content_prompt" to contentJson
                    )
                )
                successMessage = "Упражнението е запазено в библиотеката!"
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeacherExerciseScreen(
    onBack: () -> Unit,
    viewModel: CreateTeacherExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ново упражнение") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
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

            // ── Section 1: Basic info ────────────────────────────
            Text(
                "Основна информация",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Заглавие на упражнението") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = viewModel.instructions,
                onValueChange = { viewModel.instructions = it },
                label = { Text("Инструкции за ученика (на български)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(12.dp))

            // Speaking toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Упражнение за говорене (Speaking)",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = viewModel.isSpeaking,
                    onCheckedChange = { viewModel.isSpeaking = it }
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Section 2: Questions ─────────────────────────────
            Text(
                if (viewModel.isSpeaking) "Изречения за произнасяне"
                else "Въпроси и отговори",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            viewModel.questions.forEachIndexed { index, _ ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (viewModel.questions.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeQuestion(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, "Изтрий",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = viewModel.questions[index],
                            onValueChange = { viewModel.questions[index] = it },
                            label = {
                                Text(
                                    if (viewModel.isSpeaking) "Изречение на английски"
                                    else "Въпрос / Изречение на английски"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!viewModel.isSpeaking) {
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = viewModel.answers[index],
                                onValueChange = { viewModel.answers[index] = it },
                                label = { Text("Правилен отговор на английски") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Add question button
            OutlinedButton(
                onClick = { viewModel.addQuestion() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Добави въпрос")
            }

            Spacer(Modifier.height(24.dp))

            // ── Status messages ──────────────────────────────────
            if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (viewModel.successMessage.isNotEmpty()) {
                Text(
                    viewModel.successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── Save button ──────────────────────────────────────
            Button(
                onClick = {
                    viewModel.saveExercise(tokenManager = tokenManager, onSuccess = onBack)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !viewModel.isSaving
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("ЗАПАЗИ В БИБЛИОТЕКАТА", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}