package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.viktor.englishapp.domain.ExerciseResponse
import kotlinx.coroutines.launch

// 1. ViewModel, който се грижи за тегленето и изтриването на упражненията
class ExpertActiveViewModel : ViewModel() {
    var exercises by mutableStateOf<List<ExerciseResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    // 🟢 ДОБАВЯМЕ token КАТО ПАРАМЕТЪР
    fun loadActiveExercises(token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                // 🟢 ПОДАВАМЕ ТОКЕНА КЪМ ЗАЯВКАТА
                exercises = RetrofitClient.instance.getExercises("Bearer $token")
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteExercise(token: String, id: Int) {
        viewModelScope.launch {
            try {
                // Използваме ендпойнта за отказ/изтриване
                RetrofitClient.instance.rejectExercise(id, "Bearer $token")
                exercises = exercises.filter { it.id != id } // Премахваме го от списъка веднага
            } catch (e: Exception) {
                errorMessage = "Грешка при изтриване: ${e.message}"
            }
        }
    }
}

// 2. Самият екран
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertActiveExercisesScreen(
    onBack: () -> Unit,
    viewModel: ExpertActiveViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken()

    LaunchedEffect(Unit) {
        // 🟢 ПРОВЕРЯВАМЕ ДАЛИ ИМА ТОКЕН И ГО ПОДАВАМЕ
        if (token != null) {
            viewModel.loadActiveExercises(token)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Всички Активни Упражнения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else if (viewModel.exercises.isEmpty()) {
                Text(
                    text = "Няма активни упражнения в системата.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.exercises) { exercise ->
                        ActiveExerciseCard(
                            exercise = exercise,
                            onDelete = {
                                if (token != null) viewModel.deleteExercise(token, exercise.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

// 3. Карта за всяко упражнение
@Composable
fun ActiveExerciseCard(exercise: ExerciseResponse, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val gson = remember { Gson() }

    // Опитваме да декодираме съдържанието
    val parsedExercise = remember(exercise.content_prompt) {
        try {
            var cleanJson = exercise.content_prompt.trim()
            if (!cleanJson.startsWith("{") && !cleanJson.startsWith("`")) {
                val decodedBytes = android.util.Base64.decode(cleanJson, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                cleanJson = String(decodedBytes, Charsets.UTF_8).trim()
            }
            if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
            else if (cleanJson.startsWith("```")) cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()

            gson.fromJson(cleanJson, ExerciseContent::class.java)
        } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заглавна част
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = exercise.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "№${exercise.id} | Статус: Активно", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }

                // Бутон за изтриване (кошче)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Изтрий", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Бутон за разгъване/сгъване
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Скрий съдържанието" else "Преглед на съдържанието")
            }

            // Разгънато съдържание (само за четене)
            if (expanded && parsedExercise != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Инструкции:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(parsedExercise.instructions ?: "Няма инструкции", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(12.dp))

                Text(if (parsedExercise.is_speaking) "Текстове за произнасяне:" else "Въпроси и Отговори:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                parsedExercise.content?.forEachIndexed { index, question ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${index + 1}. $question", style = MaterialTheme.typography.bodyMedium)

                    if (!parsedExercise.is_speaking && parsedExercise.correct_answers != null && index < parsedExercise.correct_answers.size) {
                        Text("Отговор: ${parsedExercise.correct_answers[index]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else if (expanded && parsedExercise == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Грешка при разчитане на съдържанието.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

