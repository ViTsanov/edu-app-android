package com.viktor.englishapp.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch
import com.viktor.englishapp.domain.ClassroomItem
import com.viktor.englishapp.domain.ClassroomStudent


// ── ViewModel ─────────────────────────────────────────────────────
class ClassroomViewModel : ViewModel() {
    var classrooms by mutableStateOf<List<ClassroomItem>>(emptyList())
    var selectedClassroom by mutableStateOf<ClassroomItem?>(null)
    var students by mutableStateOf<List<ClassroomStudent>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf("")
    var successMessage by mutableStateOf("")

    fun loadClassrooms(token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.instance.getTeacherClassrooms("Bearer $token")
                classrooms = response
            } catch (e: Exception) {
                error = "Грешка: ${e.message}"
            } finally { isLoading = false }
        }
    }

    fun createClassroom(token: String, name: String, levelId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.createClassroom(
                    token = "Bearer $token",
                    body = mapOf("name" to name, "level_id" to levelId)
                )
                successMessage = "Класът е създаден!"
                loadClassrooms(token)
            } catch (e: Exception) {
                error = "Грешка при създаване: ${e.message}"
            }
        }
    }

    fun loadStudents(token: String, classroomId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.instance.getClassroomStudents(
                    classroomId = classroomId,
                    token = "Bearer $token"
                )
                students = response
            } catch (e: Exception) {
                error = "Грешка: ${e.message}"
            } finally { isLoading = false }
        }
    }

    fun removeStudent(token: String, classroomId: Int, studentId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.removeStudentFromClassroom(
                    classroomId = classroomId,
                    studentId = studentId,
                    token = "Bearer $token"
                )
                students = students.filter { it.id != studentId }
                successMessage = "Ученикът е премахнат."
            } catch (e: Exception) {
                error = "Грешка: ${e.message}"
            }
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomManagementScreen(
    onBack: () -> Unit,
    onNavigateToMonitoring: (Int) -> Unit,
    viewModel: ClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    val token = remember { TokenManager(context).getToken() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (token != null) viewModel.loadClassrooms(token)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моите класове") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Нов клас")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                viewModel.classrooms.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.School, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Нямате класове още.", color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showCreateDialog = true }) { Text("Създай клас") }
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.successMessage.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        viewModel.successMessage,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        items(viewModel.classrooms) { classroom ->
                            ClassroomCard(
                                classroom = classroom,
                                onViewStudents = {
                                    viewModel.selectedClassroom = classroom
                                    if (token != null) viewModel.loadStudents(token, classroom.id)
                                },
                                onMonitoring = { onNavigateToMonitoring(classroom.id) }
                            )
                        }
                    }
                }
            }

            // Student list bottom sheet
            if (viewModel.selectedClassroom != null) {
                StudentListSheet(
                    classroom = viewModel.selectedClassroom!!,
                    students = viewModel.students,
                    onDismiss = { viewModel.selectedClassroom = null },
                    onRemove = { studentId ->
                        if (token != null)
                            viewModel.removeStudent(token, viewModel.selectedClassroom!!.id, studentId)
                    }
                )
            }
        }
    }

    // Create classroom dialog
    if (showCreateDialog) {
        CreateClassroomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, levelId ->
                if (token != null) viewModel.createClassroom(token, name, levelId)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ClassroomCard(
    classroom: ClassroomItem,
    onViewStudents: () -> Unit,
    onMonitoring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(classroom.name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${classroom.student_count} ученика · Ниво ${classroom.level_id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Access code chip
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Код: ${classroom.access_code}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewStudents,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ученици")
                }
                Button(
                    onClick = onMonitoring,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Analytics, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Мониторинг")
                }
            }
        }
    }
}

@Composable
private fun StudentListSheet(
    classroom: ClassroomItem,
    students: List<ClassroomStudent>,
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ученици в ${classroom.name}") },
        text = {
            if (students.isEmpty()) {
                Text("Няма записани ученици.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(students) { student ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.username, fontWeight = FontWeight.Bold)
                                Text(
                                    "${student.english_level} · ${student.total_xp} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            IconButton(onClick = { onRemove(student.id) }) {
                                Icon(Icons.Default.PersonRemove, "Премахни",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Затвори") }
        }
    )
}

@Composable
private fun CreateClassroomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var levelId by remember { mutableStateOf(1) }
    val levels = listOf("A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Създай нов клас") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Име на класа") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Ниво на класа:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levels.forEach { (label, id) ->
                        FilterChip(
                            selected = levelId == id,
                            onClick = { levelId = id },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, levelId) },
                enabled = name.isNotBlank()
            ) { Text("Създай") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отказ") }
        }
    )
}