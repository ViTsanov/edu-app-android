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
import com.viktor.englishapp.data.RetrofitClient
import com.viktor.englishapp.data.TokenManager
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────

data class TeacherTest(
    val id: Int,
    val title: String,
    val description: String,
    val classroomId: Int,
    val timeLimitMinutes: Int,
    val isActive: Boolean,
    val opensAt: String?,
    val exerciseCount: Int,
    val attemptCount: Int,
    val createdAt: String
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class TestManagementViewModel : ViewModel() {

    var tests by mutableStateOf<List<TeacherTest>>(emptyList())
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")
    var statusMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance.getTeacherTests("Bearer $token")
                tests = response.map { map ->
                    TeacherTest(
                        id = (map["id"] as? Double)?.toInt() ?: 0,
                        title = map["title"] as? String ?: "",
                        description = map["description"] as? String ?: "",
                        classroomId = (map["classroom_id"] as? Double)?.toInt() ?: 0,
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

    fun activateTest(
        tokenManager: TokenManager,
        testId: Int,
        opensAt: String?          // null = immediately, ISO string = scheduled
    ) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: return@launch
                val body: Map<String, Any?> = if (opensAt != null)
                    mapOf("opens_at" to opensAt)
                else
                    mapOf("opens_at" to null)

                RetrofitClient.instance.activateTestWithTime(
                    testId = testId,
                    token = "Bearer $token",
                    body = body
                )
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
}

// ─────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────

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

    // Activation dialog state
    var dialogTestId by remember { mutableStateOf<Int?>(null) }

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
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Нямате тестове.", color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onCreateTest) { Text("Създай тест") }
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.statusMessage.isNotEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        viewModel.statusMessage,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        items(viewModel.tests) { test ->
                            TestManagementCard(
                                test = test,
                                onActivate = { dialogTestId = test.id },
                                onDeactivate = {
                                    viewModel.deactivateTest(tokenManager, test.id)
                                },
                                onViewResults = { onViewResults(test.id) }
                            )
                        }
                    }
                }
            }

            // Activation dialog
            dialogTestId?.let { testId ->
                ActivationDialog(
                    onDismiss = { dialogTestId = null },
                    onActivateNow = {
                        viewModel.activateTest(tokenManager, testId, null)
                        dialogTestId = null
                    },
                    onActivateScheduled = { dateTimeStr ->
                        viewModel.activateTest(tokenManager, testId, dateTimeStr)
                        dialogTestId = null
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Test management card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TestManagementCard(
    test: TeacherTest,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onViewResults: () -> Unit
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
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Title + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(test.title, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    if (test.description.isNotBlank()) {
                        Text(
                            test.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
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

            // Info row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(Icons.Default.Quiz, "${test.exerciseCount} упражнения")
                InfoChip(Icons.Default.People, "${test.attemptCount} опита")
                if (test.timeLimitMinutes > 0) {
                    InfoChip(Icons.Default.Timer, "${test.timeLimitMinutes} мин.")
                }
            }

            // Opening time
            if (test.opensAt != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule, null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFD97706)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Отваря: ${test.opensAt.take(16).replace("T", " ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!test.isActive) {
                    Button(
                        onClick = onActivate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Активирай")
                    }
                } else {
                    OutlinedButton(
                        onClick = onDeactivate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Деактивирай")
                    }
                }

                OutlinedButton(
                    onClick = onViewResults,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Резултати")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.secondary)
    }
}

// ─────────────────────────────────────────────────────────────────
// Activation dialog — choose immediate or scheduled
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ActivationDialog(
    onDismiss: () -> Unit,
    onActivateNow: () -> Unit,
    onActivateScheduled: (String) -> Unit
) {
    var showScheduled by remember { mutableStateOf(false) }

    // Simple date+time text fields
    // Format: YYYY-MM-DDTHH:MM  e.g. "2024-06-15T09:00"
    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("09:00") }
    var inputError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Активирай теста") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!showScheduled) {
                    Text(
                        "Изберете кога тестът да стане достъпен за учениците.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onActivateNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FlashOn, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Веднага")
                    }
                    OutlinedButton(
                        onClick = { showScheduled = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("На точен час и дата")
                    }
                } else {
                    Text("Въведи датата и часа на отваряне:")
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it; inputError = "" },
                        label = { Text("Дата (ГГГГ-ММ-ДД)") },
                        placeholder = { Text("2024-06-15") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                        }
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it; inputError = "" },
                        label = { Text("Час (ЧЧ:ММ)") },
                        placeholder = { Text("09:00") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp))
                        }
                    )
                    if (inputError.isNotEmpty()) {
                        Text(inputError, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    // Preview
                    if (dateText.length == 10 && timeText.length == 5) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Тестът ще се отвори на $dateText в $timeText",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showScheduled) {
                Button(onClick = {
                    if (dateText.length != 10) {
                        inputError = "Формат: ГГГГ-ММ-ДД"
                        return@Button
                    }
                    if (timeText.length != 5) {
                        inputError = "Формат: ЧЧ:ММ"
                        return@Button
                    }
                    onActivateScheduled("${dateText}T${timeText}:00")
                }) {
                    Text("Планирай")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (showScheduled) showScheduled = false
                else onDismiss()
            }) {
                Text(if (showScheduled) "Назад" else "Отказ")
            }
        }
    )
}