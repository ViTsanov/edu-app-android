package com.viktor.englishapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
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

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class ExpertActiveViewModel : ViewModel() {
    var exercises by mutableStateOf<List<ExerciseResponse>>(emptyList())
    var teacherExercises by mutableStateOf<List<Map<String, Any>>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf("")          // AI tab errors only
    var teacherLoadError by mutableStateOf("")       // My exercises tab errors only
    var isTeacherRole by mutableStateOf(true)        // false = expert, hide Tab 1

    fun loadActiveExercises(token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                exercises = RetrofitClient.instance.getExercises("Bearer $token")
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Грешка при зареждане: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadTeacherExercises(token: String) {
        viewModelScope.launch {
            try {
                teacherExercises = RetrofitClient.instance.getTeacherExercises("Bearer $token")
                isTeacherRole = true
                teacherLoadError = ""
            } catch (e: Exception) {
                // 403 = user is an Expert, not a Teacher — hide Tab 1 silently
                if (e.message?.contains("403") == true || e.message?.contains("forbidden", ignoreCase = true) == true) {
                    isTeacherRole = false
                } else {
                    teacherLoadError = "Грешка: ${e.message}"
                }
            }
        }
    }

    fun deleteExercise(token: String, id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.rejectExercise(id, "Bearer $token")
                exercises = exercises.filter { it.id != id }
            } catch (e: Exception) {
                errorMessage = "Грешка при изтриване: ${e.message}"
            }
        }
    }

    fun deleteTeacherExercise(token: String, id: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.deleteTeacherExercise(id, "Bearer $token")
                teacherExercises = teacherExercises.filter {
                    (it["id"] as? Double)?.toInt() != id
                }
            } catch (e: Exception) {
                teacherLoadError = "Грешка при изтриване: ${e.message}"
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertActiveExercisesScreen(
    onBack: () -> Unit,
    onEditTeacherExercise: ((Int, String, String) -> Unit)? = null,
    viewModel: ExpertActiveViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = tokenManager.getToken()
    var selectedTab by remember { mutableStateOf(0) }
    // Show Tab 1 only for teachers (experts get 403 — detected in ViewModel)
    val tabs = if (viewModel.isTeacherRole)
        listOf("📋 Одобрени упражнения", "📚 Моите")
    else
        listOf("📍 Одобрени упражнения")

    LaunchedEffect(Unit) {
        if (token != null) {
            viewModel.loadActiveExercises(token)
            viewModel.loadTeacherExercises(token)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Активни Упражнения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AiExercisesTab(
                    viewModel = viewModel,
                    token = token
                )
                1 -> if (viewModel.isTeacherRole) MyExercisesTab(
                    viewModel = viewModel,
                    token = token,
                    onEdit = onEditTeacherExercise
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Tab 0: AI exercises
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AiExercisesTab(viewModel: ExpertActiveViewModel, token: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            viewModel.errorMessage.isNotEmpty() -> Text(
                viewModel.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            viewModel.exercises.isEmpty() -> Text(
                "Няма активни упражнения в системата.",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.exercises) { exercise ->
                    ActiveExerciseCard(
                        exercise = exercise,
                        onDelete = { if (token != null) viewModel.deleteExercise(token, exercise.id) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Tab 1: Teacher's own exercises
// ─────────────────────────────────────────────────────────────────

@Composable
private fun MyExercisesTab(
    viewModel: ExpertActiveViewModel,
    token: String?,
    onEdit: ((Int, String, String) -> Unit)?
) {
    if (viewModel.teacherLoadError.isNotEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(viewModel.teacherLoadError, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp))
        }
        return
    }
    if (viewModel.teacherExercises.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(12.dp))
                Text("Нямате създадени упражнения още.", color = MaterialTheme.colorScheme.secondary)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(viewModel.teacherExercises) { ex ->
            val id = (ex["id"] as? Double)?.toInt() ?: 0
            val title = ex["title"] as? String ?: ""
            val contentJson = ex["content_prompt"] as? String ?: ""
            val createdAt = (ex["created_at"] as? String)?.take(10) ?: ""

            TeacherExerciseCard(
                id = id,
                title = title,
                contentJson = contentJson,
                createdAt = createdAt,
                onEdit = {
                    onEdit?.invoke(id, title, contentJson)
                },
                onDelete = {
                    if (token != null) viewModel.deleteTeacherExercise(token, id)
                }
            )
        }
    }
}

@Composable
private fun TeacherExerciseCard(
    id: Int,
    title: String,
    contentJson: String,
    createdAt: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val gson = remember { Gson() }
    val parsed = remember(contentJson) {
        try { gson.fromJson(contentJson, ExerciseContent::class.java) } catch (_: Exception) { null }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Изтриване на упражнение") },
            text = { Text("Сигурен ли си, че искаш да изтриеш «$title»? Това действие не може да се отмени.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Изтрий", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Откажи") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Създадено: $createdAt • ID #$id",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                // Edit button
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Редактирай", tint = MaterialTheme.colorScheme.primary)
                }
                // Delete button
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Изтрий", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Скрий съдържанието" else "Преглед на съдържанието")
            }

            if (expanded && parsed != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text("Инструкции:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(parsed.instructions ?: "Няма", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (parsed.is_speaking) "Изречения:" else "Въпроси:",
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                )
                parsed.content?.forEachIndexed { i, q ->
                    Text("${i + 1}. $q", style = MaterialTheme.typography.bodySmall)
                    if (!parsed.is_speaking) {
                        val ans = parsed.correct_answers?.getOrNull(i) ?: ""
                        if (ans.isNotBlank()) {
                            Text("→ $ans", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// AI exercise card (unchanged)
// ─────────────────────────────────────────────────────────────────

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
            } else if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Грешка при разчитане на съдържанието.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

