package com.viktor.englishapp.ui

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
import kotlinx.coroutines.launch

data class TeacherTest(
    val id: Int,
    val title: String,
    val description: String,
    val classroomId: Int,
    val classroomName: String,   // ← ново поле
    val timeLimitMinutes: Int,
    val isActive: Boolean,
    val opensAt: String?,
    val exerciseCount: Int,
    val attemptCount: Int,
    val createdAt: String
)

class TestManagementViewModel : ViewModel() {

    var tests by mutableStateOf<List<TeacherTest>>(emptyList())
    var classrooms by mutableStateOf<List<Pair<Int, String>>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")
    var statusMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch

                // Load classrooms for name lookup
                val cls = RetrofitClient.instance.getTeacherClassrooms("Bearer $token")
                classrooms = cls.map { it.id to it.name }

                val response = RetrofitClient.instance.getTeacherTests("Bearer $token")
                tests = response.map { map ->
                    val classroomId = (map["classroom_id"] as? Double)?.toInt() ?: 0
                    val classroomName = cls.find { it.id == classroomId }?.name ?: "Клас #$classroomId"
                    TeacherTest(
                        id = (map["id"] as? Double)?.toInt() ?: 0,
                        title = map["title"] as? String ?: "",
                        description = map["description"] as? String ?: "",
                        classroomId = classroomId,
                        classroomName = classroomName,
                        timeLimitMinutes = (map["time_limit_minutes"] as? Double)?.toInt() ?: 0,
                        isActive = map["is_active"] as? Boolean ?: false,
                        opensAt = map["opens_at"] as? String,
                        exerciseCount = (map["exercise_count"] as? Double)?.toInt() ?: 0,
                        attemptCount = (map["attempt_count"] as? Double)?.toInt() ?: 0,
                        createdAt = map["created_at"] as? String ?: ""
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun activateTest(tokenManager: TokenManager, testId: Int, opensAt: String?) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                val body: Map<String, Any?> = mapOf("opens_at" to opensAt)
                RetrofitClient.instance.activateTestWithTime(testId, "Bearer $token", body)
                statusMessage = "Тестът е активиран!"
                load(tokenManager)
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            }
        }
    }

    fun deactivateTest(tokenManager: TokenManager, testId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                RetrofitClient.instance.deactivateTest(testId, "Bearer $token")
                statusMessage = "Тестът е деактивиран."
                load(tokenManager)
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            }
        }
    }

    fun reassignClassroom(tokenManager: TokenManager, testId: Int, newClassroomId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                RetrofitClient.instance.updateTestClassroom(
                    testId = testId,
                    token = "Bearer $token",
                    body = mapOf("classroom_id" to newClassroomId)
                )
                statusMessage = "Класът е сменен!"
                load(tokenManager)
            } catch (e: Exception) {
                errorMessage = "Грешка: ${e.message}"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestManagementScreen(
    onBack: () -> Unit,
    onCreateTest: () -> Unit,
    onViewResults: (Int) -> Unit,
    viewModel: TestManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.load(tokenManager) }

    var activateDialogTestId by remember { mutableStateOf<Int?>(null) }
    var reassignTest by remember { mutableStateOf<TeacherTest?>(null) }

    // Auto-clear status
    LaunchedEffect(viewModel.statusMessage) {
        if (viewModel.statusMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(2500)
            viewModel.statusMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление на тестове") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateTest) {
                        Icon(Icons.Default.Add, "Нов тест")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                viewModel.tests.isEmpty() -> {
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
                                Icon(Icons.Default.Quiz, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Нямате тестове", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Натиснете + за да създадете тест", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onCreateTest) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Създай тест")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Status banner
                        if (viewModel.statusMessage.isNotEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFF16A34A).copy(0.1f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(viewModel.statusMessage, color = Color(0xFF16A34A), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        // Summary stats
                        item {
                            val active = viewModel.tests.count { it.isActive }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TestStatCard("Тестове", viewModel.tests.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                                TestStatCard("Активни", active.toString(), if (active > 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        items(viewModel.tests) { test ->
                            TestManagementCard(
                                test = test,
                                onActivate = { activateDialogTestId = test.id },
                                onDeactivate = { viewModel.deactivateTest(tokenManager, test.id) },
                                onViewResults = { onViewResults(test.id) },
                                onReassign = { reassignTest = test }
                            )
                        }
                    }
                }
            }

            // Dialogs
            activateDialogTestId?.let { testId ->
                ActivationDialog(
                    onDismiss = { activateDialogTestId = null },
                    onActivateNow = {
                        viewModel.activateTest(tokenManager, testId, null)
                        activateDialogTestId = null
                    },
                    onActivateScheduled = { dt ->
                        viewModel.activateTest(tokenManager, testId, dt)
                        activateDialogTestId = null
                    }
                )
            }

            reassignTest?.let { test ->
                ReassignClassroomDialog(
                    currentClassroomId = test.classroomId,
                    classrooms = viewModel.classrooms,
                    onDismiss = { reassignTest = null },
                    onConfirm = { newId ->
                        viewModel.reassignClassroom(tokenManager, test.id, newId)
                        reassignTest = null
                    }
                )
            }
        }
    }
}

// ── Test card ─────────────────────────────────────────────────────
@Composable
private fun TestManagementCard(
    test: TeacherTest,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onViewResults: () -> Unit,
    onReassign: () -> Unit
) {
    val statusColor = when {
        test.isActive && test.opensAt == null -> Color(0xFF16A34A)
        test.isActive && test.opensAt != null -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.secondary
    }
    val statusLabel = when {
        test.isActive && test.opensAt == null -> "Активен"
        test.isActive && test.opensAt != null -> "Планиран"
        else -> "Неактивен"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Title + status badge
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(test.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (test.description.isNotBlank()) {
                        Text(test.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                    }
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraLarge) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Classroom chip — тапване отваря диалог за смяна
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                onClick = onReassign
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(5.dp))
                    Text(test.classroomName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, null, Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info chips
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(Icons.Default.Quiz, "${test.exerciseCount} упр.")
                InfoChip(Icons.Default.People, "${test.attemptCount} опита")
                if (test.timeLimitMinutes > 0) InfoChip(Icons.Default.Timer, "${test.timeLimitMinutes} мин.")
            }

            if (test.opensAt != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color(0xFFD97706))
                    Spacer(Modifier.width(4.dp))
                    Text("Отваря: ${test.opensAt.take(16).replace("T", " ")}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(0.12f))
            Spacer(Modifier.height(10.dp))

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!test.isActive) {
                    Button(onClick = onActivate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Активирай")
                    }
                } else {
                    OutlinedButton(
                        onClick = onDeactivate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Деактивирай")
                    }
                }
                OutlinedButton(onClick = onViewResults, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Резултати")
                }
            }
        }
    }
}

// ── Stat card ─────────────────────────────────────────────────────
@Composable
private fun TestStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(3.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

// ── Reassign classroom dialog ─────────────────────────────────────
@Composable
private fun ReassignClassroomDialog(
    currentClassroomId: Int,
    classrooms: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedId by remember { mutableStateOf(currentClassroomId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Смени класа")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Избери класа, за който е предназначен тестът:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                classrooms.forEach { (id, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedId == id, onClick = { selectedId = id })
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selectedId == id) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedId) }, enabled = selectedId != currentClassroomId) {
                Text("Запази")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отказ") }
        }
    )
}

// ── Activation dialog ─────────────────────────────────────────────
@Composable
private fun ActivationDialog(
    onDismiss: () -> Unit,
    onActivateNow: () -> Unit,
    onActivateScheduled: (String) -> Unit
) {
    var showScheduled by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("09:00") }
    var inputError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Активирай теста") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!showScheduled) {
                    Text("Изберете кога тестът да стане достъпен.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onActivateNow, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FlashOn, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Веднага")
                    }
                    OutlinedButton(onClick = { showScheduled = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("На точен час и дата")
                    }
                } else {
                    Text("Въведи датата и часа:")
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it; inputError = "" }, label = { Text("Дата (ГГГГ-ММ-ДД)") }, placeholder = { Text("2024-06-15") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = timeText, onValueChange = { timeText = it; inputError = "" }, label = { Text("Час (ЧЧ:ММ)") }, placeholder = { Text("09:00") }, modifier = Modifier.fillMaxWidth())
                    if (inputError.isNotEmpty()) Text(inputError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    if (dateText.length == 10 && timeText.length == 5) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                            Text("Отваря на $dateText в $timeText", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showScheduled) {
                Button(onClick = {
                    if (dateText.length != 10) { inputError = "Формат: ГГГГ-ММ-ДД"; return@Button }
                    if (timeText.length != 5) { inputError = "Формат: ЧЧ:ММ"; return@Button }
                    onActivateScheduled("${dateText}T${timeText}:00")
                }) { Text("Планирай") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (showScheduled) showScheduled = false else onDismiss() }) {
                Text(if (showScheduled) "Назад" else "Отказ")
            }
        }
    )
}
