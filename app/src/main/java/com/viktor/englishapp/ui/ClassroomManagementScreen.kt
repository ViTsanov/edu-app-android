package com.viktor.englishapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import com.viktor.englishapp.domain.ClassroomItem
import com.viktor.englishapp.domain.ClassroomStudent
import kotlinx.coroutines.launch

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
                classrooms = RetrofitClient.instance.getTeacherClassrooms("Bearer $token")
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
                students = RetrofitClient.instance.getClassroomStudents(classroomId, "Bearer $token")
            } catch (e: Exception) {
                error = "Грешка: ${e.message}"
            } finally { isLoading = false }
        }
    }

    fun removeStudent(token: String, classroomId: Int, studentId: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.removeStudentFromClassroom(classroomId, studentId, "Bearer $token")
                students = students.filter { it.id != studentId }
                successMessage = "Ученикът е премахнат."
            } catch (e: Exception) {
                error = "Грешка: ${e.message}"
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomManagementScreen(
    onBack: () -> Unit,
    onNavigateToMonitoring: (Int) -> Unit,
    onNavigateToDetail: (Int) -> Unit = {},
    viewModel: ClassroomViewModel = viewModel()
) {
    val context = LocalContext.current
    val token = remember { TokenManager(context).getToken() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (token != null) viewModel.loadClassrooms(token) }

    // Success snackbar
    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(2000)
            viewModel.successMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моите класове") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Нов клас") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                viewModel.classrooms.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.School, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Нямате класове", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Натиснете + за да създадете клас", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Success banner
                        if (viewModel.successMessage.isNotEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF16A34A).copy(0.1f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(viewModel.successMessage, color = Color(0xFF16A34A), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Summary
                        item {
                            val totalStudents = viewModel.classrooms.sumOf { it.student_count }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TeacherStatCard("Класове", viewModel.classrooms.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                                TeacherStatCard("Ученици", totalStudents.toString(), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        items(viewModel.classrooms) { classroom ->
                            TeacherClassroomCard(
                                classroom = classroom,
                                onOpen = { onNavigateToDetail(classroom.id) },
                                onViewStudents = {
                                    viewModel.selectedClassroom = classroom
                                    if (token != null) viewModel.loadStudents(token, classroom.id)
                                    showStudentDialog = true
                                },
                                onMonitoring = { onNavigateToMonitoring(classroom.id) }
                            )
                        }

                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }

            // Student list dialog
            if (showStudentDialog && viewModel.selectedClassroom != null) {
                StudentListDialog(
                    classroom = viewModel.selectedClassroom!!,
                    students = viewModel.students,
                    isLoading = viewModel.isLoading,
                    onDismiss = { showStudentDialog = false; viewModel.selectedClassroom = null },
                    onRemove = { studentId ->
                        if (token != null)
                            viewModel.removeStudent(token, viewModel.selectedClassroom!!.id, studentId)
                    }
                )
            }
        }
    }

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

// ── Classroom card ────────────────────────────────────────────────
@Composable
private fun TeacherClassroomCard(
    classroom: ClassroomItem,
    onOpen: () -> Unit,
    onViewStudents: () -> Unit,
    onMonitoring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            classroom.name.firstOrNull()?.uppercaseChar()?.toString() ?: "К",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(classroom.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${classroom.student_count} ученика",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                val levelName = when (classroom.level_id) {
                    1 -> "A1"; 2 -> "A2"; 3 -> "B1"; 4 -> "B2"; 5 -> "C1"; else -> "A1"
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        levelName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Access code
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Код за достъп:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                    Text(classroom.access_code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.15f))
            Spacer(Modifier.height(10.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewStudents,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ученици")
                }
                Button(
                    onClick = onMonitoring,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Мониторинг")
                }
            }
        }
    }
}

// ── Stat card ─────────────────────────────────────────────────────
@Composable
private fun TeacherStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// ── Student list dialog ───────────────────────────────────────────
@Composable
private fun StudentListDialog(
    classroom: ClassroomItem,
    students: List<ClassroomStudent>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit
) {
    var confirmRemoveId by remember { mutableStateOf<Int?>(null) }

    if (confirmRemoveId != null) {
        AlertDialog(
            onDismissRequest = { confirmRemoveId = null },
            title = { Text("Премахни ученик") },
            text = { Text("Сигурен ли си, че искаш да премахнеш този ученик от класа?") },
            confirmButton = {
                TextButton(onClick = { onRemove(confirmRemoveId!!); confirmRemoveId = null }) {
                    Text("Премахни", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveId = null }) { Text("Отказ") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("${classroom.name} — ученици")
            }
        },
        text = {
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (students.isEmpty()) {
                Text("Няма записани ученици в този клас.", color = MaterialTheme.colorScheme.secondary)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(students) { student ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        student.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.username, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${student.english_level} · ${student.total_xp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(
                                onClick = { confirmRemoveId = student.id },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonRemove, "Премахни",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (student != students.last()) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.12f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Затвори") }
        }
    )
}

// ── Create classroom dialog ───────────────────────────────────────
@Composable
private fun CreateClassroomDialog(onDismiss: () -> Unit, onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var levelId by remember { mutableStateOf(1) }
    val levels = listOf("A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Нов клас")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Име на класа") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, Modifier.size(16.dp)) }
                )

                Column {
                    Text("Ниво на класа", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        levels.forEach { (label, id) ->
                            FilterChip(
                                selected = levelId == id,
                                onClick = { levelId = id },
                                label = { Text(label, fontWeight = if (levelId == id) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
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
