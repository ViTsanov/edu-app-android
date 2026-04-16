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
import com.viktor.englishapp.domain.StudentPathItem
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────

data class HomeworkItem(
    val id: Int,
    val title: String,
    val description: String,
    val classroomName: String,
    val exerciseId: Int?,
    val teacherExerciseId: Int?,
    val contentPrompt: String?,
    val dueDate: String?,
    val completed: Boolean,
    val overdue: Boolean
)

// ─────────────────────────────────────────────────────────────────
// Student ViewModel
// ─────────────────────────────────────────────────────────────────

class StudentHomeworkViewModel : ViewModel() {

    var homework by mutableStateOf<List<HomeworkItem>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance.getStudentHomework("Bearer $token")
                homework = response.map { map ->
                    HomeworkItem(
                        id = (map["id"] as? Double)?.toInt() ?: 0,
                        title = map["title"] as? String ?: "",
                        description = map["description"] as? String ?: "",
                        classroomName = map["classroom_name"] as? String ?: "",
                        exerciseId = (map["exercise_id"] as? Double)?.toInt(),
                        teacherExerciseId = (map["teacher_exercise_id"] as? Double)?.toInt(),
                        contentPrompt = map["content_prompt"] as? String,
                        dueDate = map["due_date"] as? String,
                        completed = map["completed"] as? Boolean ?: false,
                        overdue = map["overdue"] as? Boolean ?: false
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
// Student homework screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeworkScreen(
    onBack: () -> Unit,
    onStartExercise: (Int, String) -> Unit,  // exerciseId, encodedJson
    viewModel: StudentHomeworkViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.load(tokenManager) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Домашни") },
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
            viewModel.homework.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AssignmentTurnedIn,
                            null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Нямаш домашни в момента",
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
                    // Summary chips
                    item {
                        val pending = viewModel.homework.count { !it.completed }
                        val overdue = viewModel.homework.count { it.overdue }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (pending > 0) {
                                SummaryChip("$pending чакащи", Color(0xFF1E40AF))
                            }
                            if (overdue > 0) {
                                SummaryChip("$overdue просрочени", Color(0xFFDC2626))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    items(viewModel.homework) { hw ->
                        HomeworkCard(
                            hw = hw,
                            onStart = {
                                val json = hw.contentPrompt ?: return@HomeworkCard
                                val encoded = java.net.URLEncoder.encode(json, "UTF-8")
                                val exId = hw.exerciseId ?: hw.teacherExerciseId ?: 0
                                onStartExercise(exId, encoded)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun HomeworkCard(hw: HomeworkItem, onStart: () -> Unit) {
    val bgColor = when {
        hw.completed -> Color(0xFFF0FDF4)
        hw.overdue -> Color(0xFFFFF5F5)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        hw.completed -> Color(0xFF16A34A)
        hw.overdue -> Color(0xFFDC2626)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(hw.title, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    Text(
                        hw.classroomName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                when {
                    hw.completed -> Icon(
                        Icons.Default.CheckCircle, null,
                        tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp)
                    )
                    hw.overdue -> Icon(
                        Icons.Default.Warning, null,
                        tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp)
                    )
                    else -> Icon(
                        Icons.Default.Assignment, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Due date
            if (hw.dueDate != null) {
                Spacer(Modifier.height(6.dp))
                val dueParts = hw.dueDate.take(10)  // "2024-06-15"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, null,
                        modifier = Modifier.size(12.dp),
                        tint = if (hw.overdue) Color(0xFFDC2626)
                        else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "До: $dueParts",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hw.overdue) Color(0xFFDC2626)
                        else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (!hw.completed && hw.contentPrompt != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (hw.overdue)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    else ButtonDefaults.buttonColors()
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hw.overdue) "Предай (закъснение)" else "Реши домашното")
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────
// Teacher — Assign homework screen
// ─────────────────────────────────────────────────────────────────

class AssignHomeworkViewModel : ViewModel() {

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedClassroomId by mutableStateOf<Int?>(null)
    var selectedClassroomName by mutableStateOf("Изберете клас")
    var selectedExerciseId by mutableStateOf<Int?>(null)
    var selectedExerciseName by mutableStateOf("Изберете упражнение")
    var dueDateText by mutableStateOf("")

    var classrooms by mutableStateOf<List<Map<String, Any>>>(emptyList())
    var exercises by mutableStateOf<List<Map<String, Any>>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
    var errorMessage by mutableStateOf("")
    var successMessage by mutableStateOf("")

    fun loadData(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val cl = RetrofitClient.instance.getTeacherClassrooms("Bearer $token")
                classrooms = cl.map { mapOf("id" to it.id, "name" to it.name) }

                val ex = RetrofitClient.instance.getExercises("Bearer $token")
                exercises = ex.map { mapOf("id" to it.id, "title" to it.title) }
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun assign(tokenManager: TokenManager, onSuccess: () -> Unit) {
        if (title.isBlank()) { errorMessage = "Въведи заглавие."; return }
        if (selectedClassroomId == null) { errorMessage = "Изберете клас."; return }
        if (selectedExerciseId == null) { errorMessage = "Изберете упражнение."; return }

        isSaving = true
        errorMessage = ""
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                RetrofitClient.instance.createHomework(
                    token = "Bearer $token",
                    body = buildMap {
                        put("title", title)
                        put("description", description)
                        put("classroom_id", selectedClassroomId!!)
                        put("exercise_id", selectedExerciseId!!)
                        if (dueDateText.isNotBlank()) put("due_date", dueDateText + "T23:59:00")
                    }
                )
                successMessage = "Домашното е зададено!"
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignHomeworkScreen(
    onBack: () -> Unit,
    viewModel: AssignHomeworkViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var showClassroomMenu by remember { mutableStateOf(false) }
    var showExerciseMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadData(tokenManager) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Задай домашно") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Заглавие на домашното") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Описание (незадължително)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Classroom picker
            Box {
                OutlinedButton(
                    onClick = { showClassroomMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(viewModel.selectedClassroomName)
                }
                DropdownMenu(
                    expanded = showClassroomMenu,
                    onDismissRequest = { showClassroomMenu = false }
                ) {
                    viewModel.classrooms.forEach { cl ->
                        val id = (cl["id"] as? Int) ?: 0
                        val name = cl["name"] as? String ?: ""
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                viewModel.selectedClassroomId = id
                                viewModel.selectedClassroomName = name
                                showClassroomMenu = false
                            }
                        )
                    }
                }
            }

            // Exercise picker
            Box {
                OutlinedButton(
                    onClick = { showExerciseMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Assignment, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(viewModel.selectedExerciseName)
                }
                DropdownMenu(
                    expanded = showExerciseMenu,
                    onDismissRequest = { showExerciseMenu = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    viewModel.exercises.forEach { ex ->
                        val id = (ex["id"] as? Int) ?: 0
                        val title = ex["title"] as? String ?: ""
                        DropdownMenuItem(
                            text = { Text(title, maxLines = 2) },
                            onClick = {
                                viewModel.selectedExerciseId = id
                                viewModel.selectedExerciseName = title
                                showExerciseMenu = false
                            }
                        )
                    }
                }
            }

            // Due date (simple text input — format YYYY-MM-DD)
            OutlinedTextField(
                value = viewModel.dueDateText,
                onValueChange = { viewModel.dueDateText = it },
                label = { Text("Краен срок (ГГГГ-ММ-ДД, незадължително)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("2024-06-15") },
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                }
            )

            if (viewModel.errorMessage.isNotEmpty()) {
                Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            if (viewModel.successMessage.isNotEmpty()) {
                Text(viewModel.successMessage, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.assign(tokenManager = tokenManager, onSuccess = onBack)
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
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ЗАДАЙ ДОМАШНОТО", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}