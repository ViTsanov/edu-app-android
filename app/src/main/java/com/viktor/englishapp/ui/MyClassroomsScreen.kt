package com.viktor.englishapp.ui

import androidx.compose.foundation.clickable
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

// ─────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────

data class StudentClassroom(
    val id: Int,
    val name: String,
    val accessCode: String,
    val teacherName: String,
    val level: String,
    val classmateCount: Int,
    val homeworkCount: Int,
    val joinedAt: String?
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class MyClassroomsViewModel : ViewModel() {

    var classrooms by mutableStateOf<List<StudentClassroom>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance.getStudentClassrooms("Bearer $token")
                classrooms = response.map { map ->
                    StudentClassroom(
                        id = (map["id"] as? Double)?.toInt() ?: 0,
                        name = map["name"] as? String ?: "",
                        accessCode = map["access_code"] as? String ?: "",
                        teacherName = map["teacher_name"] as? String ?: "",
                        level = map["level"] as? String ?: "A1",
                        classmateCount = (map["classmate_count"] as? Double)?.toInt() ?: 0,
                        homeworkCount = (map["homework_count"] as? Double)?.toInt() ?: 0,
                        joinedAt = map["joined_at"] as? String
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
// Screen
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyClassroomsScreen(
    onBack: () -> Unit,
    onGoToHomework: () -> Unit,
    onGoToJoin: () -> Unit,
    onOpenClassroom: (Int) -> Unit,
    viewModel: MyClassroomsViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.load(tokenManager) }

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
                    // Quick access to homework
                    IconButton(onClick = onGoToHomework) {
                        Icon(Icons.Default.Assignment, "Домашни")
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
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            viewModel.classrooms.isEmpty() -> {
                // Empty state
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.School,
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Не си в нито един клас",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Поискай код от учителя и се присъедини",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onGoToJoin) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Присъедини се към клас")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Join new class button at top
                    item {
                        OutlinedButton(
                            onClick = onGoToJoin,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Присъедини се към нов клас")
                        }
                    }

                    items(viewModel.classrooms) { classroom ->
                        StudentClassroomCard(
                            classroom = classroom,
                            onGoToHomework = onGoToHomework,
                            onOpenClassroom = { onOpenClassroom(classroom.id) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Classroom card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StudentClassroomCard(
    classroom: StudentClassroom,
    onGoToHomework: () -> Unit,
    onOpenClassroom: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenClassroom() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            classroom.name.firstOrNull()?.toString() ?: "К",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        classroom.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Учител: ${classroom.teacherName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                // Level badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        classroom.level,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ClassroomStat(
                    icon = Icons.Default.People,
                    value = "${classroom.classmateCount}",
                    label = "ученика"
                )
                ClassroomStat(
                    icon = Icons.Default.Assignment,
                    value = "${classroom.homeworkCount}",
                    label = "домашни"
                )
            }

            // Homework button if there are assignments
            if (classroom.homeworkCount > 0) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGoToHomework,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Виж домашните (${classroom.homeworkCount})")
                }
            }
        }
    }
}

@Composable
private fun ClassroomStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}