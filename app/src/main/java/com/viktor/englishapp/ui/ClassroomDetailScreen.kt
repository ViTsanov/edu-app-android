package com.viktor.englishapp.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
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
// Data models
// ─────────────────────────────────────────────────────────────────

data class ClassroomDetailData(
    val id: Int,
    val name: String,
    val level: String,
    val teacherName: String,
    val classmateCount: Int,
    val homework: List<HomeworkDetailItem>,
    val tests: List<ClassroomTestItem>
)

data class HomeworkDetailItem(
    val id: Int,
    val title: String,
    val description: String,
    val exerciseId: Int?,
    val teacherExerciseId: Int?,
    val contentPrompt: String?,
    val dueDate: String?,
    val submitted: Boolean,
    val score: Int?,
    val overdue: Boolean,
    val submittedAt: String?
)

data class ClassroomTestItem(
    val id: Int,
    val title: String,
    val description: String,
    val timeLimitMinutes: Int,
    val exerciseCount: Int,
    val isOpen: Boolean,
    val opensAt: String?,
    val isCompleted: Boolean,
    val myScore: Int?
)

// ─────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────

class ClassroomDetailViewModel : ViewModel() {

    var detail by mutableStateOf<ClassroomDetailData?>(null)
    var isLoading by mutableStateOf(true)
    var errorMessage by mutableStateOf("")

    fun load(tokenManager: TokenManager, classroomId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                val token = tokenManager.getToken() ?: return@launch
                val response = RetrofitClient.instance
                    .getClassroomDetail(classroomId, "Bearer $token")

                @Suppress("UNCHECKED_CAST")
                val hwList = (response["homework"] as? List<Map<String, Any?>>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val testList = (response["tests"] as? List<Map<String, Any?>>) ?: emptyList()

                detail = ClassroomDetailData(
                    id = (response["id"] as? Double)?.toInt() ?: classroomId,
                    name = response["name"] as? String ?: "",
                    level = response["level"] as? String ?: "A1",
                    teacherName = response["teacher_name"] as? String ?: "",
                    classmateCount = (response["classmate_count"] as? Double)?.toInt() ?: 0,
                    homework = hwList.map { hw ->
                        HomeworkDetailItem(
                            id = (hw["id"] as? Double)?.toInt() ?: 0,
                            title = hw["title"] as? String ?: "",
                            description = hw["description"] as? String ?: "",
                            exerciseId = (hw["exercise_id"] as? Double)?.toInt(),
                            teacherExerciseId = (hw["teacher_exercise_id"] as? Double)?.toInt(),
                            contentPrompt = hw["content_prompt"] as? String,
                            dueDate = hw["due_date"] as? String,
                            submitted = hw["submitted"] as? Boolean ?: false,
                            score = (hw["score"] as? Double)?.toInt(),
                            overdue = hw["overdue"] as? Boolean ?: false,
                            submittedAt = hw["submitted_at"] as? String
                        )
                    },
                    tests = testList.map { t ->
                        ClassroomTestItem(
                            id = (t["id"] as? Double)?.toInt() ?: 0,
                            title = t["title"] as? String ?: "",
                            description = t["description"] as? String ?: "",
                            timeLimitMinutes = (t["time_limit_minutes"] as? Double)?.toInt() ?: 0,
                            exerciseCount = (t["exercise_count"] as? Double)?.toInt() ?: 0,
                            isOpen = t["is_open"] as? Boolean ?: false,
                            opensAt = t["opens_at"] as? String,
                            isCompleted = t["is_completed"] as? Boolean ?: false,
                            myScore = (t["my_score"] as? Double)?.toInt()
                        )
                    }
                )
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
fun ClassroomDetailScreen(
    classroomId: Int,
    onBack: () -> Unit,
    onOpenHomework: (HomeworkDetailItem) -> Unit,
    onOpenTest: (Int) -> Unit,
    viewModel: ClassroomDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(classroomId) { viewModel.load(tokenManager, classroomId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.detail?.name ?: "Клас") },
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
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(viewModel.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            viewModel.detail != null -> {
                val detail = viewModel.detail!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    // ── Class info header ─────────────────────────
                    ClassroomHeader(detail = detail)

                    // ── Tab row ───────────────────────────────────
                    val pendingHw = detail.homework.count { !it.submitted }
                    val tabs = listOf(
                        "Домашни${if (pendingHw > 0) " ($pendingHw)" else ""}",
                        "Тестове (${detail.tests.size})"
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { i, title ->
                            Tab(
                                selected = selectedTab == i,
                                onClick = { selectedTab = i },
                                text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }

                    // ── Tab content ───────────────────────────────
                    when (selectedTab) {
                        0 -> HomeworkTab(
                            homework = detail.homework,
                            onOpen = onOpenHomework
                        )
                        1 -> TestsTab(
                            tests = detail.tests,
                            onOpen = onOpenTest
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Classroom info header
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ClassroomHeader(detail: ClassroomDetailData) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        detail.name.firstOrNull()?.toString() ?: "К",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Column {
                Text(
                    detail.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Учител: ${detail.teacherName}  ·  Ниво ${detail.level}  ·  ${detail.classmateCount} ученика",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Homework tab
// ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeworkTab(
    homework: List<HomeworkDetailItem>,
    onOpen: (HomeworkDetailItem) -> Unit
) {
    if (homework.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AssignmentTurnedIn,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(8.dp))
                Text("Няма домашни в момента", color = MaterialTheme.colorScheme.secondary)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary row
        item {
            val pending = homework.count { !it.submitted }
            val done = homework.count { it.submitted }
            val overdue = homework.count { it.overdue }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (done > 0) StatusChip("$done предадени", Color(0xFF16A34A))
                if (pending > 0) StatusChip("$pending чакащи", Color(0xFF2563EB))
                if (overdue > 0) StatusChip("$overdue просрочени", Color(0xFFDC2626))
            }
        }

        items(homework) { hw ->
            HomeworkDetailCard(hw = hw, onOpen = { onOpen(hw) })
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun HomeworkDetailCard(
    hw: HomeworkDetailItem,
    onOpen: () -> Unit
) {
    val bgColor = when {
        hw.submitted -> Color(0xFFF0FDF4)
        hw.overdue -> Color(0xFFFFF5F5)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        hw.submitted -> Color(0xFF16A34A)
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
                    Text(
                        hw.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (hw.description.isNotBlank()) {
                        Text(
                            hw.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 2
                        )
                    }
                }
                when {
                    hw.submitted -> Icon(
                        Icons.Default.CheckCircle, null,
                        tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp)
                    )
                    hw.overdue -> Icon(
                        Icons.Default.Warning, null,
                        tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp)
                    )
                    else -> Icon(
                        Icons.Default.RadioButtonUnchecked, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Score or due date
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hw.submitted && hw.score != null) {
                    Surface(
                        color = if (hw.score >= 70) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            "${hw.score}/100",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hw.score >= 70) Color(0xFF16A34A) else Color(0xFFD97706)
                        )
                    }
                    hw.submittedAt?.let { at ->
                        Text(
                            "Предадено: ${at.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    hw.dueDate?.let { due ->
                        Icon(
                            Icons.Default.CalendarToday, null,
                            modifier = Modifier.size(12.dp),
                            tint = if (hw.overdue) Color(0xFFDC2626)
                            else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "До: ${due.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hw.overdue) Color(0xFFDC2626)
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Action button
            if (!hw.submitted && hw.contentPrompt != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (hw.overdue)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    else ButtonDefaults.buttonColors()
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hw.overdue) "Предай (закъснение)"
                        else "Реши домашното",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (hw.submitted) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Виж отговорите си")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Tests tab
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TestsTab(
    tests: List<ClassroomTestItem>,
    onOpen: (Int) -> Unit
) {
    if (tests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Quiz,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(8.dp))
                Text("Няма активни тестове", color = MaterialTheme.colorScheme.secondary)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tests) { test ->
            TestDetailCard(test = test, onOpen = { onOpen(test.id) })
        }
    }
}

@Composable
private fun TestDetailCard(
    test: ClassroomTestItem,
    onOpen: () -> Unit
) {
    val bgColor = when {
        test.isCompleted -> Color(0xFFF0FDF4)
        !test.isOpen -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        test.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (test.description.isNotBlank()) {
                        Text(
                            test.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }
                when {
                    test.isCompleted -> Icon(
                        Icons.Default.CheckCircle, null,
                        tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp)
                    )
                    !test.isOpen -> Icon(
                        Icons.Default.Schedule, null,
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp)
                    )
                    else -> Icon(
                        Icons.Default.PlayArrow, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TestInfoChip(Icons.Default.Quiz, "${test.exerciseCount} упр.")
                if (test.timeLimitMinutes > 0) {
                    TestInfoChip(Icons.Default.Timer, "${test.timeLimitMinutes} мин.")
                }
                if (test.isCompleted && test.myScore != null) {
                    TestInfoChip(Icons.Default.Grade, "${test.myScore}/100")
                }
            }

            if (!test.isOpen && test.opensAt != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Отваря: ${test.opensAt.take(16).replace("T", " ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (test.isOpen && !test.isCompleted) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Започни теста", fontWeight = FontWeight.Bold)
                }
            } else if (test.isCompleted) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Завършен  ·  ${test.myScore}/100",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TestInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}