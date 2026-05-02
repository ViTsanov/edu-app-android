package com.viktor.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
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
// STUDENT — ViewModel
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
// STUDENT — screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeworkScreen(
    onBack: () -> Unit,
    onStartExercise: (Int, String) -> Unit,
    viewModel: StudentHomeworkViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.load(tokenManager) }

    val pending = viewModel.homework.count { !it.completed && !it.overdue }
    val overdue = viewModel.homework.count { it.overdue && !it.completed }
    val done = viewModel.homework.count { it.completed }

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
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Нямаш домашни!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Всичко е наред.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stats row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HomeworkStatCard(
                                label = "Чакащи",
                                value = pending.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            HomeworkStatCard(
                                label = "Просрочени",
                                value = overdue.toString(),
                                color = if (overdue > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            HomeworkStatCard(
                                label = "Предадени",
                                value = done.toString(),
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Overdue first
                    val overdueList = viewModel.homework.filter { it.overdue && !it.completed }
                    val pendingList = viewModel.homework.filter { !it.completed && !it.overdue }
                    val doneList = viewModel.homework.filter { it.completed }

                    if (overdueList.isNotEmpty()) {
                        item { HomeworkSectionHeader("Просрочени", Color(0xFFDC2626)) }
                        items(overdueList) { hw ->
                            StudentHwCard(hw = hw, onStart = {
                                val json = hw.contentPrompt ?: return@StudentHwCard
                                val encoded = java.net.URLEncoder.encode(json, "UTF-8")
                                val exId = hw.exerciseId ?: hw.teacherExerciseId ?: 0
                                onStartExercise(exId, encoded)
                            })
                        }
                    }

                    if (pendingList.isNotEmpty()) {
                        item { HomeworkSectionHeader("За предаване", MaterialTheme.colorScheme.primary) }
                        items(pendingList) { hw ->
                            StudentHwCard(hw = hw, onStart = {
                                val json = hw.contentPrompt ?: return@StudentHwCard
                                val encoded = java.net.URLEncoder.encode(json, "UTF-8")
                                val exId = hw.exerciseId ?: hw.teacherExerciseId ?: 0
                                onStartExercise(exId, encoded)
                            })
                        }
                    }

                    if (doneList.isNotEmpty()) {
                        item { HomeworkSectionHeader("Предадени", Color(0xFF16A34A)) }
                        items(doneList) { hw -> StudentHwCard(hw = hw, onStart = {}) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeworkStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun HomeworkSectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 18.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StudentHwCard(hw: HomeworkItem, onStart: () -> Unit) {
    val statusColor = when {
        hw.completed -> Color(0xFF16A34A)
        hw.overdue -> Color(0xFFDC2626)
        else -> MaterialTheme.colorScheme.primary
    }
    val statusIcon = when {
        hw.completed -> Icons.Default.CheckCircle
        hw.overdue -> Icons.Default.Warning
        else -> Icons.AutoMirrored.Filled.Assignment
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (hw.completed) Color(0xFF16A34A).copy(0.3f)
            else if (hw.overdue) Color(0xFFDC2626).copy(0.3f)
            else MaterialTheme.colorScheme.outline.copy(0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Status icon circle
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(hw.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(hw.classroomName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                if (hw.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(hw.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 2)
                }

                hw.dueDate?.let { due ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule, null,
                            modifier = Modifier.size(12.dp),
                            tint = if (hw.overdue) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "До ${due.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hw.overdue) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (!hw.completed && hw.contentPrompt != null) {
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (hw.overdue) Color(0xFFDC2626).copy(0.1f) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (hw.overdue) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (hw.overdue) "Предай (закъснение)" else "Реши домашното", fontWeight = FontWeight.Bold)
                    }
                }

                if (hw.completed) {
                    Spacer(Modifier.height(6.dp))
                    Text("✓ Предадено успешно", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// TEACHER — homework list ViewModel
// ─────────────────────────────────────────────────────────────────

data class TeacherHomeworkListItem(
    val id: Int,
    val title: String,
    val description: String,
    val classroomName: String,
    val classroomId: Int,
    val contentPrompt: String?,
    val dueDate: String?,
    val studentCount: Int,
    val completedCount: Int
)

class TeacherHomeworkListViewModel : ViewModel() {

    var homework by mutableStateOf<List<TeacherHomeworkListItem>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance.getTeacherHomework("Bearer $token")
                homework = response.map { map ->
                    TeacherHomeworkListItem(
                        id = (map["id"] as? Double)?.toInt() ?: 0,
                        title = map["title"] as? String ?: "",
                        description = map["description"] as? String ?: "",
                        classroomName = map["classroom_name"] as? String ?: "",
                        classroomId = (map["classroom_id"] as? Double)?.toInt() ?: 0,
                        contentPrompt = map["content_prompt"] as? String,
                        dueDate = map["due_date"] as? String,
                        studentCount = (map["student_count"] as? Double)?.toInt() ?: 0,
                        completedCount = (map["completed_count"] as? Double)?.toInt() ?: 0
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
// TEACHER — homework list screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeworkListScreen(
    onBack: () -> Unit,
    onAssignNew: () -> Unit,
    onViewSubmissions: (Int, String, String) -> Unit,
    viewModel: TeacherHomeworkListViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.load(tokenManager) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Домашни на класа") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAssignNew,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Ново домашно") }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            viewModel.errorMessage.isNotEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            viewModel.homework.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Нямате зададени домашни", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Натиснете + за да зададете първото", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp, ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary
                    item {
                        val totalStudents = viewModel.homework.sumOf { it.studentCount }
                        val totalDone = viewModel.homework.sumOf { it.completedCount }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeworkStatCard("Домашни", viewModel.homework.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                            HomeworkStatCard("Предали", "$totalDone/$totalStudents", Color(0xFF16A34A), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    items(viewModel.homework) { hw ->
                        TeacherHwCard(
                            hw = hw,
                            onViewSubmissions = {
                                val encTitle = java.net.URLEncoder.encode(hw.title, "UTF-8")
                                val encJson = java.net.URLEncoder.encode(hw.contentPrompt ?: "", "UTF-8")
                                onViewSubmissions(hw.id, encTitle, encJson)
                            }
                        )
                    }

                    // FAB clearance
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TeacherHwCard(hw: TeacherHomeworkListItem, onViewSubmissions: () -> Unit) {
    val progress = if (hw.studentCount > 0) hw.completedCount.toFloat() / hw.studentCount else 0f
    val allDone = hw.completedCount == hw.studentCount && hw.studentCount > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            hw.classroomName.firstOrNull()?.toString() ?: "К",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(hw.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(hw.classroomName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                // Progress badge
                Surface(
                    color = if (allDone) Color(0xFF16A34A).copy(0.1f) else MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        "${hw.completedCount}/${hw.studentCount}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (allDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Progress bar
            if (hw.studentCount > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.extraLarge),
                    color = if (allDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(0.12f)
                )
            }

            // Due date
            hw.dueDate?.let { due ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("До ${due.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.15f))
            Spacer(Modifier.height(10.dp))

            // Action button
            OutlinedButton(onClick = onViewSubmissions, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.People, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Виж отговорите")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// TEACHER — Assign homework screen
// ─────────────────────────────────────────────────────────────────

class AssignHomeworkViewModel : ViewModel() {

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var selectedClassroomId by mutableStateOf<Int?>(null)
    var selectedClassroomName by mutableStateOf("Изберете клас")
    var selectedExerciseId by mutableStateOf<Int?>(null)
    var selectedExerciseName by mutableStateOf("Без упражнение (незадължително)")
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
                        if (selectedExerciseId != null) put("exercise_id", selectedExerciseId!!)
                        if (dueDateText.isNotBlank()) put("due_date", "${dueDateText}T23:59:00")
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
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Основна информация", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            item {
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text("Заглавие на домашното *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Title, null, Modifier.size(18.dp)) }
                )
            }
            item {
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Описание (незадължително)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }

            item { Spacer(Modifier.height(4.dp)); Text("Настройки", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }

            // Class picker
            item {
                Box {
                    OutlinedTextField(
                        value = viewModel.selectedClassroomName,
                        onValueChange = {},
                        label = { Text("Клас *") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.School, null, Modifier.size(18.dp)) },
                        trailingIcon = { Icon(Icons.Default.ExpandMore, null) },
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    // Invisible clickable overlay
                    Surface(
                        modifier = Modifier.matchParentSize(),
                        color = Color.Transparent,
                        onClick = { showClassroomMenu = true }
                    ) {}
                    DropdownMenu(expanded = showClassroomMenu, onDismissRequest = { showClassroomMenu = false }) {
                        viewModel.classrooms.forEach { cl ->
                            val id = (cl["id"] as? Int) ?: 0
                            val name = cl["name"] as? String ?: ""
                            DropdownMenuItem(
                                text = { Text(name) },
                                leadingIcon = { Icon(Icons.Default.School, null, Modifier.size(16.dp)) },
                                onClick = {
                                    viewModel.selectedClassroomId = id
                                    viewModel.selectedClassroomName = name
                                    showClassroomMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Exercise picker
            item {
                Box {
                    OutlinedTextField(
                        value = viewModel.selectedExerciseName,
                        onValueChange = {},
                        label = { Text("Упражнение (незадължително)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.LibraryBooks, null, Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (viewModel.selectedExerciseId != null) {
                                IconButton(onClick = {
                                    viewModel.selectedExerciseId = null
                                    viewModel.selectedExerciseName = "Без упражнение (незадължително)"
                                }) { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) }
                            } else Icon(Icons.Default.ExpandMore, null)
                        }
                    )
                    if (viewModel.selectedExerciseId == null) {
                        Surface(modifier = Modifier.matchParentSize(), color = Color.Transparent, onClick = { showExerciseMenu = true }) {}
                    }
                    DropdownMenu(expanded = showExerciseMenu, onDismissRequest = { showExerciseMenu = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                        DropdownMenuItem(
                            text = { Text("Без упражнение", color = MaterialTheme.colorScheme.secondary) },
                            leadingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) },
                            onClick = {
                                viewModel.selectedExerciseId = null
                                viewModel.selectedExerciseName = "Без упражнение (незадължително)"
                                showExerciseMenu = false
                            }
                        )
                        HorizontalDivider()
                        viewModel.exercises.forEach { ex ->
                            val id = (ex["id"] as? Int) ?: 0
                            val exTitle = ex["title"] as? String ?: ""
                            DropdownMenuItem(
                                text = { Text(exTitle, maxLines = 2) },
                                onClick = {
                                    viewModel.selectedExerciseId = id
                                    viewModel.selectedExerciseName = exTitle
                                    showExerciseMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Due date
            item {
                OutlinedTextField(
                    value = viewModel.dueDateText,
                    onValueChange = { viewModel.dueDateText = it },
                    label = { Text("Краен срок (ГГГГ-ММ-ДД)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("напр. 2024-06-15") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp)) }
                )
            }

            if (viewModel.errorMessage.isNotEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Button(
                    onClick = { viewModel.assign(tokenManager = tokenManager, onSuccess = onBack) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !viewModel.isSaving
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Задай домашното", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Shared chip (kept for back-compat)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraLarge) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
